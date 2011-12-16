package serwer;

import java.io.IOException;
import java.util.concurrent.*;

/**
 *
 * @author jk
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // zajmie się wykonywaniem wątku serwera; pojedynczego
        Executor watekSerwera = Executors.newSingleThreadExecutor();

        int port = 10103;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);


        Serwer s;
        try
        {
            s = new Serwer(port);    
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }


        // uruchom serwer (główny wątek) -> uruchomi metodę run()
        watekSerwera.execute(s);

    }

}
