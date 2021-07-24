package tp.farming_springboot.domain.user.dto;

import lombok.Setter;
import lombok.Getter;

public class UserDto {
    @Getter
    public static class UserRegisterDto{
        private String phone;
        private String address;
    }
    @Getter
    public static class UserLoginDto{
        private String phone;
        private int otp;
    }
    @Getter
    public static class UserRequestOtpDto{
        private String phone;
    }
    @Getter
    public static class UserAuthDto{
        private String phone;
    }
    @Getter
    public static class UserNewAddressDto{
        private String content;
        private Double lat;
        private Double lon;
    }
}
