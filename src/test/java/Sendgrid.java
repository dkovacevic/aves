import com.aves.server.tools.Logger;
import com.aves.server.tools.Util;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class Sendgrid {
    @Test
    public void testEmail() throws IOException {
        InputStream resourceAsStream = Sendgrid.class.getClassLoader().getResourceAsStream("template.html");
        String body = new String(Util.toByteArray(resourceAsStream));

        String email = "dejan@wire.com";
        String user = "Tiaguška";
        String password = "123";
        body = body.replace("[USER]", user).replace("[EMAIL]", email).replace("[PASSWORD]", password);

        boolean b = Util.sendEmail("Your New Account", body, "aves@wire.com", email);
        if (b)
            Logger.info("Email sent");
    }
}
