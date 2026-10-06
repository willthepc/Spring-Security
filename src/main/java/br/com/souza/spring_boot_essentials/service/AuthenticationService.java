package br.com.souza.spring_boot_essentials.service;

import br.com.souza.spring_boot_essentials.config.TokenProvider;
import br.com.souza.spring_boot_essentials.database.model.AlunosEntity;
import br.com.souza.spring_boot_essentials.database.model.RolesEntity;
import br.com.souza.spring_boot_essentials.database.repository.IAlunosRepository;
import br.com.souza.spring_boot_essentials.database.repository.IRolesRepository;
import br.com.souza.spring_boot_essentials.dto.AlunoDto;
import br.com.souza.spring_boot_essentials.dto.LoginRequestDto;
import br.com.souza.spring_boot_essentials.dto.RegisterRequestDto;
import br.com.souza.spring_boot_essentials.dto.TokenResponseDto;
import br.com.souza.spring_boot_essentials.enums.RoleTypeEnum;
import br.com.souza.spring_boot_essentials.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final IAlunosRepository alunosRepository;
    private final IRolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    @Value("${jwt.expiration}")
    private long expirationTime;

    public void register(RegisterRequestDto dto) throws BadRequestException {
        if (alunosRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BadRequestException("Email já cadastrado");
        }

        RolesEntity role = rolesRepository.findByNome(RoleTypeEnum.ROLE_ALUNO.name())
                .orElseGet(() -> rolesRepository.save(RolesEntity.builder()
                        .nome(RoleTypeEnum.ROLE_ALUNO.name())
                        .build()));

        alunosRepository.save(AlunosEntity.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .roles(Set.of(role))
                .senha(passwordEncoder.encode(dto.getSenha()))
                .build());
    }

    public TokenResponseDto login (LoginRequestDto dto) throws Exception {
        try {
           Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getSenha()));
           String token = tokenProvider.gerarToken(authentication);

           return new TokenResponseDto(token, expirationTime);
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Credenciais Inválidas.");
        } catch (Exception e) {
            throw new RuntimeException("Algo deu errado.");
        }
    }
}
