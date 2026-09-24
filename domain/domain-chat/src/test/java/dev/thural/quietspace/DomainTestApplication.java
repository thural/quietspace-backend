package dev.thural.quietspace;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test-scope bootstrap for web-slice tests ({@code @WebMvcTest}) in library
 * modules, which own no production application class by design.
 */
@SpringBootApplication
public class DomainTestApplication {
}
