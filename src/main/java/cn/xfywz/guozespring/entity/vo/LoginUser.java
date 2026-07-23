package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.entity.mhmain.SlManage;
import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private List<String> list;
    private SlManage slManage;
    private YeeManage yeeManage;
    /**
     * '数据权限: 1:所有数据, 2:院级数据,3:普通数据'
     */
    private Integer dataAuth;

    public LoginUser(SlManage slManage,List<String> list) {
        this.slManage = slManage;
        this.list = list;
    }
    public LoginUser(YeeManage yeeManage,List<String> list) {
        this.yeeManage = yeeManage;
        this.list = list;
    }

    List<SimpleGrantedAuthority> authorities;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities!=null){
            return authorities;
        }
        authorities=new ArrayList<>();
        for (String s : list){
            authorities.add(new SimpleGrantedAuthority(s));
        }
        return authorities;
//        return List.of();
    }

    @Override
    public String getPassword() {
        if (yeeManage!=null){
            return yeeManage.getPassword();
        }
        return slManage.getPassword();
    }

    @Override
    public String getUsername() {
        if (yeeManage!=null){
            return yeeManage.getAccount();
        }
        return slManage.getAccount();
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
        return true;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        if (yeeManage!=null){
            return yeeManage.getIsLock() != 1;
        }
        return slManage.getIsLock() != 1;
    }
}
