package com.codename1.mojo.exec;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.StreamConsumer;

public class EnvStreamConsumer
    implements StreamConsumer
{

    public static final String START_PARSING_INDICATOR =
        "================================This is the beginning of env parsing================================";

    private Map<String, String> envs = new HashMap<String, String>();

    private boolean startParsing = false;

    public void consumeLine( String line )
    {

        if ( line.startsWith( START_PARSING_INDICATOR ) )
        {
            this.startParsing = true;
            return;
        }

        if ( this.startParsing )
        {
            String[] tokens = StringUtils.split( line, "=" );
            if ( tokens.length == 2 )
            {
                envs.put( tokens[0], tokens[1] );
            }
        }
        else
        {
            System.out.println( line );
        }

    }

    public Map<String, String> getParsedEnv()
    {
        return this.envs;
    }

}
