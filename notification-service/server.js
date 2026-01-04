const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const amqp = require('amqplib');
const cors = require('cors');
require('dotenv').config();

const app = express();
app.use(cors());

const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

const PORT = process.env.PORT || 3001;
const RABBITMQ_URL = process.env.RABBITMQ_URL || 'amqp://localhost:5672';
const SPRING_BOOT_API = process.env.SPRING_BOOT_API || 'http://localhost:8080';
const QUEUE_NAME = 'assignment-notifications';

async function getEnrolledCourses(userId) {
    try {
        const response = await fetch(`${SPRING_BOOT_API}/api/users/${userId}/enrolled-courses`);

        if (response.ok) {
            const courses = await response.json();
            return courses;
        } else if (response.status === 404) {
            return [];
        } else {
            return [];
        }
    } catch (error) {
        return [];
    }
}

const courseRooms = new Map(); // courseId -> Set of socketIds
const userSockets = new Map(); // socketId -> { userId, courseIds: [] }

io.on('connection', (socket) => {
    console.log(`Client connected: ${socket.id}`);

    socket.on('join-courses', async (data) => {
        const { userId, courseIds } = data;
        console.log(`User ${userId} requesting to join courses:`, courseIds);
        console.log(`  Course IDs type:`, typeof courseIds[0], `value:`, courseIds);

        const enrolledCourses = await getEnrolledCourses(userId);
        console.log(`User ${userId} is enrolled in:`, enrolledCourses);
        console.log(`  Enrolled courses type:`, typeof enrolledCourses[0], `value:`, enrolledCourses);

        const normalizedCourseIds = courseIds.map(id => Number(id));
        const normalizedEnrolledCourses = enrolledCourses.map(id => Number(id));

        console.log(`  Normalized requested:`, normalizedCourseIds);
        console.log(`  Normalized enrolled:`, normalizedEnrolledCourses);

        const validCourseIds = normalizedCourseIds.filter(courseId => normalizedEnrolledCourses.includes(courseId));
        const invalidCourseIds = normalizedCourseIds.filter(courseId => !normalizedEnrolledCourses.includes(courseId));

        console.log(`  Valid courses:`, validCourseIds);
        console.log(`  Invalid courses:`, invalidCourseIds);

        if (invalidCourseIds.length > 0) {
            console.log(`User ${userId} attempted to join unauthorized courses:`, invalidCourseIds);
            socket.emit('enrollment-error', {
                message: 'You are not enrolled in some of the requested courses',
                invalidCourses: invalidCourseIds
            });
        }

        userSockets.set(socket.id, { userId, courseIds: validCourseIds });

        validCourseIds.forEach(courseId => {
            const room = `course-${courseId}`;
            socket.join(room);

            if (!courseRooms.has(courseId)) {
                courseRooms.set(courseId, new Set());
            }
            courseRooms.get(courseId).add(socket.id);

            console.log(`User ${userId} joined course room ${courseId}`);
        });

        socket.emit('courses-joined', {
            enrolledCourses: validCourseIds,
            rejectedCourses: invalidCourseIds
        });
    });

    socket.on('join-course', async (data) => {
        const { userId, courseId } = data;
        console.log(`User ${userId} requesting to join course:`, courseId);

        const enrolledCourses = await getEnrolledCourses(userId);

        if (enrolledCourses.includes(Number(courseId))) {
            const room = `course-${courseId}`;
            socket.join(room);

            if (!courseRooms.has(courseId)) {
                courseRooms.set(courseId, new Set());
            }
            courseRooms.get(courseId).add(socket.id);

            const userData = userSockets.get(socket.id);
            if (userData) {
                userData.courseIds.push(Number(courseId));
            }

            console.log(`User ${userId} joined course room ${courseId}`);

            socket.emit('course-joined', {
                courseId: courseId,
                success: true
            });
        } else {
            console.log(`User ${userId} NOT enrolled in course ${courseId}`);
            socket.emit('course-join-failed', {
                courseId: courseId,
                message: 'Not enrolled in this course'
            });
        }
    });

    socket.on('leave-course', (courseId) => {
        const room = `course-${courseId}`;
        socket.leave(room);

        if (courseRooms.has(courseId)) {
            courseRooms.get(courseId).delete(socket.id);
        }

        const userData = userSockets.get(socket.id);
        if (userData) {
            userData.courseIds = userData.courseIds.filter(id => id !== Number(courseId));
        }

        console.log(`User left course room ${courseId}`);

        socket.emit('course-left', {
            courseId: courseId
        });
    });

    socket.on('disconnect', () => {
        const userData = userSockets.get(socket.id);
        if (userData) {
            userData.courseIds.forEach(courseId => {
                if (courseRooms.has(courseId)) {
                    courseRooms.get(courseId).delete(socket.id);
                }
            });
            userSockets.delete(socket.id);
        }
        console.log(`Client disconnected: ${socket.id}`);
    });
});

async function startRabbitMQConsumer() {
    try {
        const connection = await amqp.connect(RABBITMQ_URL);
        const channel = await connection.createChannel();

        await channel.assertQueue(QUEUE_NAME, { durable: true });
        console.log(`Connected to RabbitMQ. Waiting for messages in queue: ${QUEUE_NAME}`);

        channel.consume(QUEUE_NAME, (msg) => {
            if (msg !== null) {
                try {
                    const notification = JSON.parse(msg.content.toString());
                    console.log('\nReceived notification from RabbitMQ:');
                    console.log(`   Course: ${notification.courseName} (ID: ${notification.courseId})`);
                    console.log(`   Assignment: ${notification.assignmentTitle}`);
                    console.log(`   Teacher: ${notification.teacherName}`);

                    const room = `course-${notification.courseId}`;
                    const clientsInRoom = courseRooms.get(notification.courseId);
                    const clientCount = clientsInRoom ? clientsInRoom.size : 0;

                    io.to(room).emit('new-assignment', {
                        type: 'NEW_ASSIGNMENT',
                        message: `New assignment posted: ${notification.assignmentTitle}`,
                        data: notification
                    });

                    console.log(`Broadcasted to ${clientCount} client(s) in room ${room}\n`);

                    channel.ack(msg);
                } catch (error) {
                    console.error('Error processing message:', error);
                    channel.nack(msg, false, false);
                }
            }
        });

        connection.on('close', () => {
            console.error('RabbitMQ connection closed. Reconnecting...');
            setTimeout(startRabbitMQConsumer, 5000);
        });

        connection.on('error', (err) => {
            console.error('RabbitMQ connection error:', err);
        });

    } catch (error) {
        console.error('Failed to connect to RabbitMQ:', error.message);
        console.log('Retrying in 5 seconds...');
        setTimeout(startRabbitMQConsumer, 5000);
    }
}

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({
        status: 'running',
        connectedClients: io.engine.clientsCount,
        courseRooms: Array.from(courseRooms.entries()).map(([courseId, sockets]) => ({
            courseId,
            clients: sockets.size
        }))
    });
});

server.listen(PORT, () => {
    console.log(`\nNotification Service running on port ${PORT}`);
    console.log(`WebSocket endpoint: ws://localhost:${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/health\n`);

    startRabbitMQConsumer();
});

