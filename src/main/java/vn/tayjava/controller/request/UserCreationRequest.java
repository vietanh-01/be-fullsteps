package vn.tayjava.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;
import vn.tayjava.common.Gender;
import vn.tayjava.common.UserType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Getter
@ToString
public class UserCreationRequest implements Serializable {

    @NotBlank(message = "firstName not allowed blank")
    private String firstName;
    @NotBlank(message = "lastName not allowed blank")
    private String lastName;
    private Gender gender;
    private Date birthday;
    private String username;
    @Email(message = "Email invalid")
    private String email;
    private String phone;
    private UserType type;
    private List<AddressRequest> addresses; // home,office
}
