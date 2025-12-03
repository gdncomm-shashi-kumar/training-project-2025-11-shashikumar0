package com.blibli.gdn.memberService.config;

import com.blibli.gdn.memberService.domain.Member;
import com.blibli.gdn.memberService.domain.Role;
import com.blibli.gdn.memberService.repository.MemberRepository;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberDataSeeder implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        long count = memberRepository.count();
        if (count > 0) {
            log.info("Members already exist ({} found). Skipping seeding...", count);
            return;
        }

        log.info("Seeding fake Member data...");

        Faker faker = new Faker();
        List<Member> batch = new ArrayList<>();

        for (int i = 0; i < 5000; i++) {

            String email = faker.internet().emailAddress("user" + i);
            String name = faker.name().fullName();

            Member member = Member.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode("password123"))
                    .name(name)
                    .role(Role.valueOf("USER"))
                    .build();

            batch.add(member);

            // batch insert every 500
            if (i % 500 == 0 && !batch.isEmpty()) {
                memberRepository.saveAll(batch);
                batch.clear();
                log.info("Seeded {} members...", i);
            }
        }

        if (!batch.isEmpty()) {
            memberRepository.saveAll(batch);
        }

        log.info("Seeding Complete! 5000 fake members added.");
    }
}
