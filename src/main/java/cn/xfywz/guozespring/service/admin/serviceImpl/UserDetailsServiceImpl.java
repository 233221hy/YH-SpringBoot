package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlManage;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import cn.xfywz.guozespring.mapper.SlManageMapper;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// 为admin包下的服务实现类指定特定的bean名称，避免与teacher包下的同名类冲突
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SlManageMapper slManageMapper;
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (username == null || username.isEmpty()) {
            throw new UsernameNotFoundException("账号不能为空");
        }

        // 找到最后一个 @ 的位置
        int lastAt = username.lastIndexOf('@');
        if (lastAt <= 0 || lastAt == username.length() - 1) {
            throw new UsernameNotFoundException("账号格式错误：应为 '登录名@学校ID'，且学校ID为数字");
        }

        String account = username.substring(0, lastAt);      // "liming200451@163.com"
        String schoolIdStr = username.substring(lastAt + 1); // "11"

        // 校验 schoolId 是否为纯数字
        if (!schoolIdStr.matches("\\d+")) {
            throw new UsernameNotFoundException("学校ID必须为正整数");
        }
        int id = Integer.parseInt(schoolIdStr);
        if (id == 0) {
            SlManage slManage = slManageMapper.ShowAccount(account);
            if (slManage == null){
                throw new UsernameNotFoundException("用户不存在");
            }
//        return new LoginUser(slManage);
            List<String> list = new ArrayList<>();
            list.add("sl_school_list");
            return new LoginUser(slManage,list);
        }else {
            QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("allow", 1);
            queryWrapper.eq("id", id);
            List<SlSchool> slSchool = slSchoolMapper.selectList(queryWrapper);
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                connection = SlaveMysqlConnectionUtil.getConnection(slSchool.get(0));
                String sql = "select * from yee_manage where account = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, account);
                resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    YeeManage yeeManage = new YeeManage();
                    yeeManage.setId(resultSet.getInt("id"));
                    yeeManage.setName(resultSet.getString("name"));
                    yeeManage.setRole(resultSet.getInt("role"));
                    yeeManage.setEmail(resultSet.getString("email"));
                    yeeManage.setCollegeId(resultSet.getInt("collegeId"));
                    yeeManage.setColleges(resultSet.getString("colleges"));
                    yeeManage.setActive(resultSet.getInt("active"));
                    yeeManage.setAvatar(resultSet.getString("avatar"));
                    yeeManage.setMobile(resultSet.getString("mobile"));
                    yeeManage.setGender(resultSet.getString("gender"));
                    yeeManage.setIntro(resultSet.getString("intro"));
                    yeeManage.setAccount(resultSet.getString("account"));
                    yeeManage.setPassword(resultSet.getString("password"));
                    yeeManage.setSchoolId(resultSet.getInt("schoolId"));
                    List<String> list = new ArrayList<>();
                    list.add("sl_school_list");
                    return new LoginUser(yeeManage,list);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (preparedStatement != null) {
                    try {
                        preparedStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            throw new UsernameNotFoundException("学校用户不存在：" + account);
        }
    }
}
