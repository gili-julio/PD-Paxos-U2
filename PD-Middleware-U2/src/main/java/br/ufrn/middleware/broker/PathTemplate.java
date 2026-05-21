package br.ufrn.middleware.broker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compila template /kv/{key} em regex e extrai vars. */
public final class PathTemplate {
    private static final Pattern VAR = Pattern.compile("\\{([^/{}]+)}");

    private final String template;
    private final Pattern regex;
    private final List<String> varNames;

    public PathTemplate(String template) {
        this.template = template;
        var names = new ArrayList<String>();
        var m = VAR.matcher(template);
        var sb = new StringBuilder("^");
        int last = 0;
        while (m.find()) {
            sb.append(Pattern.quote(template.substring(last, m.start())));
            sb.append("([^/]+)");
            names.add(m.group(1));
            last = m.end();
        }
        sb.append(Pattern.quote(template.substring(last)));
        sb.append("$");
        this.regex = Pattern.compile(sb.toString());
        this.varNames = List.copyOf(names);
    }

    public String template() { return template; }
    public List<String> varNames() { return varNames; }

    public Map<String, String> match(String concretePath) {
        Matcher m = regex.matcher(concretePath);
        if (!m.matches()) return null;
        Map<String, String> vars = new LinkedHashMap<>();
        for (int i = 0; i < varNames.size(); i++) vars.put(varNames.get(i), m.group(i + 1));
        return vars;
    }
}
