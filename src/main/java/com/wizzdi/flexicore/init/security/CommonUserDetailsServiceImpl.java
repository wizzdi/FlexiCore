package com.wizzdi.flexicore.init.security;


import com.wizzdi.flexicore.common.user.request.CommonUserFilter;
import com.wizzdi.flexicore.common.user.service.CommonUserService;
import com.wizzdi.security.adapter.FlexicoreUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

import static java.lang.String.format;

@Component
public class CommonUserDetailsServiceImpl implements UserDetailsService {

  private final CommonUserService commonUserService;

  public CommonUserDetailsServiceImpl(CommonUserService commonUserService) {
    this.commonUserService = commonUserService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return commonUserService
        .listAllUsers(new CommonUserFilter().setEmails(Collections.singleton(username)), null)
        .stream()
        .findFirst()
        .map(f -> new FlexicoreUserDetails(f.getId(), f.getEmail(), f.getPassword()))
        .orElseThrow(() -> new UsernameNotFoundException(format("User: %s, not found", username)));
  }
}
