package course.spring.elearningplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseDto {
    @NotBlank(message = "Course name is required!")
    private String name;

    @NotBlank(message = "Course description is required!")
    private String description;

    @NotEmpty(message = "At least one category is required!")
    private List<String> categories;

    private MultipartFile image;
}