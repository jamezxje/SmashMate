package com.smashmate.security;

import com.smashmate.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var member = memberRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new UsernameNotFoundException("Member not found: " + email));
        if (member.getPassword() == null) {
            throw new UsernameNotFoundException("Member has no account: " + email);
        }
        return new User(
            member.getEmail(),
            member.getPassword(),
            List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
        );
    }
}
