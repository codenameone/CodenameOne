import com.codename1.io.JSONParser;
import com.codename1.util.StringUtil;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class JsCodenameOneCoreSliceApp {
    public static int result;

    public static void main(String[] args) throws Exception {
        int mask = 0;

        List<String> tokens = StringUtil.tokenize("alpha,beta,gamma", ',');
        if (tokens.size() == 3 && "beta".equals(tokens.get(1))) {
            mask |= 1;
        }

        if ("alpha-beta-gamma".equals(StringUtil.join(tokens, "-"))) {
            mask |= 2;
        }

        Map<String, Object> parsed = new JSONParser().parseJSON(new StringReader("{\"ok\":true,\"n\":7}"));
        if (parsed != null && parsed.containsKey("ok") && parsed.containsKey("n")) {
            mask |= 4;
        }

        result = mask;
        System.exit(mask);
    }
}
