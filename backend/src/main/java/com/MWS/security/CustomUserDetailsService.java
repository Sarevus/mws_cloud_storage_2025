/*

package com.MWS.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loaduserByUsername(String email) throws UsernameNotFoundException{
        if ("test@mail.com".equals(email)){ //времянка тест юзер
            return new CustomUserDetails(
                    1L, //user id
                    "test@mail.com", //username(мне впадлу переделывать, чтобы было ещё имя, пока онли почта)
                    "%V$^HashedPasswordExample",
                    java.util.Collections.emptyList()
            );
        }

        throw new UsernameNotFoundException("user not found with name: " + email);
    }
}


 */