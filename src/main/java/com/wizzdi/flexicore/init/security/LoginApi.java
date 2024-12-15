package com.wizzdi.flexicore.init.security;

import com.wizzdi.security.adapter.FlexicoreUserDetails;
import com.wizzdi.security.bearer.jwt.FlexicoreJwtTokenUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LoginApi")
@RestController
public class LoginApi {

  private final AuthenticationManager authenticationManager;
  private final FlexicoreJwtTokenUtil flexicoreJwtTokenUtil;

  public LoginApi(
      AuthenticationManager authenticationManager, FlexicoreJwtTokenUtil flexicoreJwtTokenUtil) {
    this.authenticationManager = authenticationManager;
    this.flexicoreJwtTokenUtil = flexicoreJwtTokenUtil;
  }

  @SecurityRequirements
  @PostMapping("login")
  public ResponseEntity<Object> login(@RequestBody @Valid LoginRequest loginRequest) {
    try {
      Authentication authenticate =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.getUsername(), loginRequest.getPassword()));
      FlexicoreUserDetails flexicoreUserDetails =
          (FlexicoreUserDetails) authenticate.getPrincipal();

      return ResponseEntity.ok()
          .header(
              HttpHeaders.AUTHORIZATION,
              flexicoreJwtTokenUtil.generateAccessToken(flexicoreUserDetails))
          .body(authenticate.getPrincipal());
    } catch (BadCredentialsException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }
}
