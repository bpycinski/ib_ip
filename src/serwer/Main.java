package serwer;

import java.io.IOException;
import java.util.concurrent.*;

/**
 *
 * @author bp
 * 
 * Znane błędy lub istotne rzeczy:
 * 
 * Jeśli nie zalogowano się, możliwe jest wpisywanie innych poleceń, ale zachowanie jest niepoprawne
 * Ustawienie AUTORECEIVE ON nie pobiera od razu oczekujących wiadomości, trzeba je ręcznie wyświetlić
 * Polecenia należy wpisywać WIELKIMI LITERAMI
 * Serwer akceptuje "przelogowanie" w trakcie trwania sesji
 * Brakuje komunikatów diagnostycznych po stronie serwera
 * 
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // zajmie się wykonywaniem wątku serwera; pojedynczego
        Executor watekSerwera = Executors.newSingleThreadExecutor();

        int port = 10105;
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
