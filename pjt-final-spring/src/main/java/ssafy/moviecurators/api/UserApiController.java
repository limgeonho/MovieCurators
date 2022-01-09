package ssafy.moviecurators.api;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ssafy.moviecurators.domain.accounts.Curator;
import ssafy.moviecurators.dto.CuratorDto;
import ssafy.moviecurators.dto.error.ErrorResponse;
import ssafy.moviecurators.dto.simple.SimpleUserDto;
import ssafy.moviecurators.service.FileService;
import ssafy.moviecurators.service.JwtTokenProvider;
import ssafy.moviecurators.domain.accounts.User;
import ssafy.moviecurators.dto.UserProfileDto;
import ssafy.moviecurators.repository.UserRepository;
import ssafy.moviecurators.service.MailService;
import ssafy.moviecurators.service.UserService;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@RestController
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MessageSource messageSource;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;


    /**
     * 회원가입, 토큰 필요 없음[POST]
     * @param request
     * @return 사용자로부터 입력받은 정보를 검증한 후 http response body에 넣어서 보냄
     */
    @PostMapping("/accounts/signup/")
    public ResponseEntity signup(@RequestBody @Validated CreateUserRequest request) {

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());

        try {
            Long id = userService.join(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CreateUserResponse(id));
        }
        catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(messageSource.getMessage("error.same.id", null, LocaleContextHolder.getLocale())));
        }
    }

    @Data
    static class CreateUserResponse {
        private Long id;
        public CreateUserResponse(Long id) {
            this.id = id;
        }
    }

    @Data
    static class CreateUserRequest {
        private String username;
        private String nickname;
        private String password;
    }


    /**
     * 로그인, JWT인증[POST]
     * @param userInfo 사용자로부터 전달받은 정보
     * @return 인증받은 정보와 토큰을 http response body에 넣어서 보냄
     */
    @PostMapping("/accounts/api-token-auth/")
    public ResponseEntity<?> login(@RequestBody Map<String, String> userInfo) {
        User user = userRepository.getByUsername(userInfo.get("username"));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(messageSource.getMessage("error.none.user", null, LocaleContextHolder.getLocale())));
        }

        if (!passwordEncoder.matches(userInfo.get("password"), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(messageSource.getMessage("error.wrong.password", null, LocaleContextHolder.getLocale())));
        }
        String token = jwtTokenProvider.createToken(user.getUsername(), user.getId());

        return ResponseEntity.ok(new LoginUserResponse(token));
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public class LoginUserResponse {
        private String token;
        public LoginUserResponse(String accessToken) {
            this.token = accessToken;
        }
    }


    /**
     * 해당 유저 이름을 가진 유저를 찾는 함수[GET]
     * @param username 조회하고 싶은 사용자의 이름
     * @return username으로 찾은 사용자 정보를 http response body에 넣어서 보냄
     */
    @GetMapping("/accounts/{username}/get_user_info/")
    public ResponseEntity getUserInfo(@PathVariable("username") String username) {
        User user = userRepository.getByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(messageSource.getMessage("error.none.user", null, LocaleContextHolder.getLocale())));
        }

        return ResponseEntity.ok().body(new UserProfileDto(user));
    }

    /**
     * 마일리지(포인트) 충전[PUT]
     * @param obj 새롭게 전달받은 포인트를 가지고 있는 collection
     * @param request
     * @return 포인트를 갱신한 정보를 http response body에 넣어서 보냄
     */
    @PutMapping("/accounts/mileage/")
    public ResponseEntity mileageChange(@RequestBody Map<String, String> obj, HttpServletRequest request) {

        String token = request.getHeader("Authorization").replaceFirst("JWT ", "");
        if(!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(messageSource.getMessage("error.valid.jwt", null, LocaleContextHolder.getLocale())));
        }
        Long userId = jwtTokenProvider.getUserIdFromJwt(token);

        Integer mileageChange = Integer.parseInt(obj.get("mileage"));

        return ResponseEntity.ok().body(new SimpleUserDto(userService.mileageChange(userId, mileageChange)));
    }

    /**
     * 후원하기[PUT]
     * @param to_userId 후원하려는 회원의 id
     * @param obj 사용자가 후원하려는 포인트의 정보를 가지고 있는 collection
     * @param request
     * @return 사용자가 후원한 금액이 적용된 회원의 정보를 http response body에 넣어서 보냄
     */
    @PutMapping("/accounts/donate/{userId}/")
    public ResponseEntity donate(@PathVariable("userId") Long to_userId,
                       @RequestBody Map<String, String> obj,
                       HttpServletRequest request) {

        String token = request.getHeader("Authorization").replaceFirst("JWT ", "");
        if(!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(messageSource.getMessage("error.valid.jwt", null, LocaleContextHolder.getLocale())));
        }
        Long from_userId = jwtTokenProvider.getUserIdFromJwt(token);

        Integer mileageChange = Integer.parseInt(obj.get("mileage"));

        userService.donate(to_userId, from_userId, mileageChange);
        mailService.sendMail(to_userId, from_userId, mileageChange);

        return ResponseEntity.ok().body(null);
    }

    /**
     * 프로필 수정[PUT]
     * @param file 사용자가 선택한 이미지 파일
     * @param nickname 사용자가 입력한 닉네임
     * @param introduction 사용자가 입력한 자기소개
     * @param request
     * @return 사용자로부터 받은 정보들을 http response body에 넣어서 보냄
     * @throws IOException
     */
    @PutMapping("/accounts/profile/")
    public ResponseEntity updateProfile(@RequestPart(value = "image", required = false) MultipartFile file,
                              @RequestPart("nickname") String nickname,
                              @RequestPart("introduction") String introduction,
                              HttpServletRequest request) throws IOException {

        String token = request.getHeader("Authorization").replaceFirst("JWT ", "");
        if(!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(messageSource.getMessage("error.valid.jwt", null, LocaleContextHolder.getLocale())));
        }
        Long userId = jwtTokenProvider.getUserIdFromJwt(token);

        User user = userRepository.getById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(messageSource.getMessage("error.none.user", null, LocaleContextHolder.getLocale())));
        }

        String image = user.getImage();
        if (file != null) {
            image = fileService.imageUploadGCS(file, user);
        }

        return ResponseEntity.ok().body(new SimpleUserDto(userService.updateProfile(userId, nickname, introduction, image)));
    }

    /**
     * 사용자가 후원한 큐레이터들 가져오기[GET]
     * @param request
     * @return 사용자가 좋아요 누른 큐레이터 목록을 http response body에 넣어서 보냄
     */
    @GetMapping("/accounts/curators/likes/")
    public ResponseEntity likesListCurator(HttpServletRequest request) {

        String token = request.getHeader("Authorization").replaceFirst("JWT ", "");
        if(!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(messageSource.getMessage("error.valid.jwt", null, LocaleContextHolder.getLocale())));
        }
        Long from_userId = jwtTokenProvider.getUserIdFromJwt(token);

        List<Curator> curators = userService.likesListCurator(from_userId);

        List<CuratorDto> result = curators.stream()
                .map(curator -> new CuratorDto(curator))
                .collect(toList());

        return ResponseEntity.ok().body(result);
    }

    /**
     * 유저 검색[GET]
     * @param request
     * @return 사용자가 검색한 큐레이터(유저)정보를 http response body에 넣어서 보냄
     */
    @GetMapping("/accounts/search/")
    public ResponseEntity<List<SimpleUserDto>> curatorSearch(HttpServletRequest request) {
        String searchKeyword = request.getParameter("searchKeyword");
        List<User> users = userService.curatorSearch(searchKeyword);
        if(users.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<SimpleUserDto> result = users.stream()
                .map(user -> new SimpleUserDto(user))
                .collect(toList());

        return ResponseEntity.ok().body(result);
    }

    /**
     * 큐레이터(유저)정보를 조회[GET]
     * @param id 조회하려는 정보를 가진 유저 id
     * @return 조회한 큐레이터(유저)의 정보를 http response body에 넣어서 보냄
     */
    @GetMapping("/accounts/curators/{userId}/")
    public ResponseEntity curatorDetail(@PathVariable("userId") Long id) {
        User user = userRepository.getById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(messageSource.getMessage("error.none.user", null, LocaleContextHolder.getLocale())));
        }

        return ResponseEntity.ok().body(new SimpleUserDto(user));
    }
}
