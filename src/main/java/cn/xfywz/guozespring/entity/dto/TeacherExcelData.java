package cn.xfywz.guozespring.entity.dto;

import cn.xfywz.guozespring.excel.ExcelDataPreprocessor;
import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 用户信息Excel导入DTO
 * 表头：登陆账号、姓名、性别、邮箱、电话号码
 */
@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
public class TeacherExcelData implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "登陆账号", index = 0)
    @NotBlank(message = "登陆账号不能为空")
    @Length(min = 4, max = 20, message = "登陆账号长度必须在4-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "登陆账号只能包含字母、数字和下划线")
    @ColumnWidth(15)
    private String loginAccount;

    @ExcelProperty(value = "姓名", index = 1)
    @NotBlank(message = "姓名不能为空")
    @Length(min = 2, max = 20, message = "姓名长度必须在2-20个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z·]+$", message = "姓名只能包含中文、英文字母和间隔号(·)")
    @ColumnWidth(15)
    private String name;

    @ExcelProperty(value = "性别", index = 2)
    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "^(男|女|未知)$", message = "性别只能为：男、女、未知")
    @ColumnWidth(10)
    private String gender;

    @ExcelProperty(value = "邮箱", index = 3)
    @Email(message = "邮箱格式不正确")
    @Length(max = 100, message = "邮箱长度不能超过100个字符")
    @ColumnWidth(25)
    private String email;

    @ExcelProperty(value = "电话号码", index = 4)
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    @ColumnWidth(15)
    private String phone;

    /**
     * 清理数据方法（可选，用于数据处理前清理）
     */
    public void cleanData() {
        this.loginAccount = ExcelDataPreprocessor.basicClean(this.loginAccount);
        this.name = ExcelDataPreprocessor.basicClean(this.name);
        this.gender = ExcelDataPreprocessor.basicClean(this.gender);
        this.email = ExcelDataPreprocessor.basicClean(this.email);
        if (this.email != null) this.email = this.email.toLowerCase();
        this.phone = ExcelDataPreprocessor.basicClean(this.phone);
    }


    @Override
    public String toString() {
        return String.format("TeacherExcelData{loginAccount='%s', name='%s', gender='%s', email='%s', phone='%s'}",
                loginAccount, name, gender, email, phone);
    }

    /**
     * 将Excel数据转换为YeeManage实体
     * @param schoolId 学校ID
     * @param collegeId 学院ID
     * @param teacherRoleId 教师角色ID
     * @param passwordEncoder 密码编码器
     * @return YeeManage实体对象
     */
    public YeeManage toYeeManage(int schoolId, Long collegeId, Long teacherRoleId, BCryptPasswordEncoder passwordEncoder) {
        YeeManage m = new YeeManage();
        m.setSchoolId(schoolId);
        m.setAddTime(new Timestamp(System.currentTimeMillis()));
        m.setAccount(this.loginAccount);
        m.setPassword(passwordEncoder.encode("a123456"));
        m.setName(this.name);
        m.setEmail(this.email);
        m.setMobile(this.phone);
        m.setGender(this.gender);
        m.setCollegeId(collegeId);
        m.setRecommend(0L);
        m.setForce(0L);
        m.setRole(teacherRoleId);
        m.setGeneral(1);
        m.setActive(1);
        return m;
    }

}
