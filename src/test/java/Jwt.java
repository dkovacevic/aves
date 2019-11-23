import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.util.UUID;

public class Jwt {

    @Test
    public void testToken() {
        String key = "ZwZAfnq2qXaOc1VUuOpcdNimACZAfLv6GaOcIhU9OpcDmi";
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes());
        UUID userId = UUID.randomUUID();

        String jwt = Jwts.builder()
                .setIssuer("https://aves.com")
                .setSubject(userId.toString())
                .signWith(secretKey)
                .compact();

        String subject = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt)
                .getBody()
                .getSubject();

        UUID uuid = UUID.fromString(subject);

    }
}
