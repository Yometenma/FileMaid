package net.filemaid.server.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.Map;
import net.filemaid.application.port.UserAccountRepository;
import net.filemaid.server.FileMaidProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserAccountRepository users;private final PasswordEncoder encoder;private final FileMaidProperties properties;
    public AuthController(UserAccountRepository users,PasswordEncoder encoder,FileMaidProperties properties){this.users=users;this.encoder=encoder;this.properties=properties;}
    @GetMapping("/status") Status status(Principal principal,CsrfToken csrf){if(csrf!=null)csrf.getToken();return new Status(properties.auth().enabled(),users.exists(),principal!=null,principal==null?null:principal.getName());}
    @PostMapping("/setup") Map<String,Boolean> setup(@Valid @RequestBody Setup request){if(!properties.auth().enabled())throw new IllegalStateException("身份验证已禁用");String username=request.username().trim();if(username.length()>64)throw new IllegalArgumentException("用户名过长");users.create(username,encoder.encode(request.password()));return Map.of("success",true);}
    @PostMapping("/change-password") Map<String,Boolean> changePassword(Principal principal,@Valid @RequestBody ChangePassword request){String username=principal.getName();var account=users.findByUsername(username).orElseThrow(()->new IllegalStateException("账号不存在"));if(!encoder.matches(request.currentPassword(),account.passwordHash()))throw new IllegalArgumentException("当前密码错误");users.updatePassword(username,encoder.encode(request.newPassword()));return Map.of("success",true);}
    public record Setup(@NotBlank String username,@Size(min=12,max=128,message="密码长度必须在 12-128 个字符之间") String password){}
    public record ChangePassword(@NotBlank String currentPassword,@NotBlank @Size(min=12,max=128,message="密码长度必须在 12-128 个字符之间") String newPassword){}
    public record Status(boolean enabled,boolean configured,boolean authenticated,String username){}
}
