package cn.xfywz.guozespring.entity.dto;

import cn.xfywz.guozespring.excel.ExcelDataPreprocessor;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
public class StudentExcelData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "学号", index = 0)
    @NotBlank(message = "学号不能为空")
    @Length(min = 1, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "学号只能包含字母和数字")
    private String number;

    @ExcelProperty(value = "姓名", index = 1)
    @NotBlank(message = "姓名不能为空")
    @Length(min = 1, max = 20)
    private String name;

    @ExcelProperty(value = "性别", index = 2)
    @NotBlank
    @Pattern(regexp = "^([男女])$", message = "性别只能是男或女")
    private String gender;

    @ExcelProperty(value = "身份证号", index = 3)
    @NotBlank
    @Length(min = 15, max = 18)
    private String idCard;

    @ExcelProperty(value = "邮箱", index = 4)
    @Email
    @Length(max = 100)
    private String email;

    @ExcelProperty(value = "电话号码", index = 5)
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "电话号码格式错误")
    private String phone;

    @ExcelProperty(value = "学院", index = 6)
    @NotBlank
    @Length(max = 100)
    private String college;

    @ExcelProperty(value = "班级", index = 7)
    @NotBlank
    @Length(max = 100)
    private String stuClass;

    @ExcelProperty(value = "年级", index = 8)
    @NotBlank
    @Pattern(regexp = "^\\d{4}$", message = "年级格式错误")
    private String grade;

    public void cleanData() {
        number = ExcelDataPreprocessor.basicClean(number);
        name = ExcelDataPreprocessor.basicClean(name);
        gender = ExcelDataPreprocessor.basicClean(gender);
        idCard = ExcelDataPreprocessor.basicClean(idCard);
        if (idCard != null) idCard = idCard.toUpperCase();
        email = ExcelDataPreprocessor.basicClean(email);
        if (email != null) email = email.toLowerCase();
        phone = ExcelDataPreprocessor.basicClean(phone);
        college = ExcelDataPreprocessor.basicClean(college);
        stuClass = ExcelDataPreprocessor.basicClean(stuClass);
        grade = ExcelDataPreprocessor.basicClean(grade);
    }

}
