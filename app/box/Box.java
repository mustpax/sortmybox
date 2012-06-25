package box;

import play.Play;

/**
 * Global values for the Box v2 API.
 */
public class Box {
    public static final String API_KEY = Play.configuration.getProperty("box.apiKey");
}
