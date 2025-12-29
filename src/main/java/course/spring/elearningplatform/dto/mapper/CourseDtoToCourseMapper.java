package course.spring.elearningplatform.dto.mapper;

import course.spring.elearningplatform.dto.CourseDto;
import course.spring.elearningplatform.dto.ImageDto;
import course.spring.elearningplatform.entity.Course;
import course.spring.elearningplatform.entity.Image;
import course.spring.elearningplatform.entity.User;
import course.spring.elearningplatform.service.ImageService;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

public class CourseDtoToCourseMapper {
    public static Course mapCourseDtoToCourse(CourseDto courseDto, User user, ImageService imageService) {
        Course course = new Course();
        course.setName(courseDto.getName());
        course.setDescription(courseDto.getDescription());
        course.setCategories(courseDto.getCategories());
        course.setCreatedBy(user);
        course.setCreatedOn(new Date());

        MultipartFile imageFile = courseDto.getImage();
        if (imageFile != null && !imageFile.isEmpty()) {
            ImageDto imageDto = new ImageDto(imageFile);
            Image savedImage = imageService.createImage(imageDto);
            course.setImage(savedImage);
        }

        return course;
    }
}