package serwer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Klasa serwera, zapewnia komunikację między wieloma klientami.
 * 
 */

public class Serwer implements Runnable {
    /** Prywatna klasa, której obiekt jest na rzecz każdego klienta po zalogowaniu
     * i przechowywany w slowniku klienci
     * przechowuje kolejkę komunikatów oczekujących na odebranie
     * @see klienci
     */
    private class KlientInfo {
        /** 
         * Kolejka oczekujących komunikatów, jej elementami sa pary 
         * < loginNadawcy, komunikat> zaimplementowane jako słowniki (java.util.Map),
         * kazdy taki słownik ma tylko 1 element
         */
        Queue < Map <String, String> > messageQueue = new LinkedList<Map<String, String>>();
        /** Czy automatycznie odbierać komunikaty po wysłaniu */
        Boolean autoReceive = false;
        /** referencja dająca dostęp do strumieni I/O klienta */
        Asystent asystent;
        
        public KlientInfo( Asystent asystent ) {
            this.asystent = asystent;
        }
                
    }
    /** Słownik przechowujący informacje nt. zalogowanych klientow 
     * Parami < Key, Value > są < loginKlienta, obiektKlientInfo >
     */
    Map<String, KlientInfo> klienci = new HashMap<String, KlientInfo>();

    /** Część dotycząca zajmowania gniazda jest identyczna, jak na laboratorium 1 */
    ServerSocket gniazdoNasluchujace = null;
    private final static int DEFAULT_PORT = 10101;
    // konstruktory klasy
    public Serwer() throws IOException    {
        this(DEFAULT_PORT);
    }
    public Serwer(int port)  throws IOException
    {
        gniazdoNasluchujace = new ServerSocket(port);
        setSocketParameters();
    }
    public Serwer(InetAddress ia, int port) throws IOException
    {
        // można też podać konkretny adres
        gniazdoNasluchujace = new ServerSocket(port, 0, ia);
        setSocketParameters();
    }
    private void setSocketParameters() throws IOException
    {
        gniazdoNasluchujace.setReuseAddress(true);
    }

    /** Kod tej metody jest taki sam, jak na laboratorium 1 - po podłączeniu
     * klienta, uruchamia metodę run() asystenta w oddzielnym wątku, a sama oczekuje
     * na kolejnych klientów
     */
    @Override
    public void run() {
        // serwer może tworzyć wiele wątków (tyle ile połączeń), więc
        // wykorzystana zostanie klasa Executors, która efektywnie obsługuje
        // taki model
        Executor watki = Executors.newCachedThreadPool();

        // tutaj tak samo, jak w serwerze jednowątkowym
        while (true)
        {
            try
            {
                Socket gniazdoPolaczenia = gniazdoNasluchujace.accept();
                //Utwórz i uruchom tylko asystenta odpowiedzialnego za odbieranie
                //komunikatów, asystent związany z wysyłaniem będzie utworzony
                //dopiero po zalogowaniu klienta
                watki.execute(new Asystent(gniazdoPolaczenia));
               
            }
            catch (IOException ioe)
            {
                System.out.println("Błąd połączenia: " + ioe.getLocalizedMessage());
                continue;
            }
        }
    }

    /** Klasa implementująca komunikację z klientem */
    private class   Asystent implements Runnable {
        /** Gniazdo przypisane połączeniu z klientem */
        private Socket gniazdoPolaczenia;
        /** login klienta wysyłającego polecenie */
        private String klientLogin=null;
        /** Bufor danych odbieranych od klienta, z którym asystent jest połączony */
        private BufferedReader daneOdbierane;
        /** Bufor danych wysyłanych do klienta, z ktorym asystent jest połączony */
        private DataOutputStream daneWysylane;
        /** Konstruktor, od razu inicjalizowany gniazdem służacym do 
         * komunikacji z właściwym klientem
         */
        private Asystent(Socket s) {
            gniazdoPolaczenia = s;
        }

        /** Główny wątek asystenta - inicjalizuje bufory danych 
         * i wchodzi w nieskończoną pętlę nasłuchiwania na połączenia
         * od klienta
         */
        @Override
        public void run() {
            try
            {
                daneOdbierane = new BufferedReader(new InputStreamReader(
                        gniazdoPolaczenia.getInputStream()
                        ));
                daneWysylane = new DataOutputStream(
                        gniazdoPolaczenia.getOutputStream()
                        );
            }
            catch (IOException e){
                System.out.print("Nie udało się połączyć z klientem. Zamykam połączenie");
                //TODO posprzątać
                return;
            }
            try
            {
                //Czekaj w nieskończonej pętli na wiadomość od klienta
                while (true)  {
                    //przetwarzaj komunikat
                    parsujPolecenie (daneOdbierane.readLine());
                }
            }
            catch (IOException e) {

            }

        }
        /** Wysyła dane do podłączonego klienta.
         * NOTE Pod sysemem Linux zmienić znak końca wiersza
         * @param string 
         */
        private void Wyslij(String string) {
            try {
                daneWysylane.writeBytes(string + "\r\n");
            } catch (IOException ex) {

            }
        }
        
        /* Obsługiwane polecenia
         * LOGIN username
         * SEND username message
         * SENDALL message
         * GET username
         * GETALL
         * AUTORECEIVE   ON / OFF (TRUE / FALSE)
         * EXIT
         */


        /** Funkcja przetwarzająca wiadomość od klienta i sterujaca dalszym
         *  przebiegiem pracy serwera
         * @param wiadomosc Wiadomość od klienta
         */
        private void parsujPolecenie(String wiadomosc) {
            /** Zmienna pomocnicza do operacji na komunikatach, 
             * zawiera pary < Login, wiadomosc>        */
            HashMap<String, String> map;
            /** login ewentualnego odbiorcy komunikatu (Dla SEND, GET) */
            String otherLogin ;
            //dzieli na komende i reszte polecenia
            String czesciWiadomosci [] = null;
            try {
                czesciWiadomosci = wiadomosc.split(" ", 2);	
                if (czesciWiadomosci.length==0)  {
                    cleanAndQuit();
                    return;
                }
            } catch (RuntimeException e) {
                cleanAndQuit();
                return;
            }
            /** Polecenie np. LOGIN, SEND ... */
            String polecenie = czesciWiadomosci[0];
            /** Ewentualna dalsza czesc komunikatu od klienta */
            String parametryPolecenia = null;
            // Konieczne sprawdzenie, bo moze rzucic wyjatkiem Runtime gdyby
            // tablica miala tylko  element
            try {
                parametryPolecenia = czesciWiadomosci[1];
            }
            catch (RuntimeException e) {
                //nic nie rob
            }
            if (polecenie.equals("LOGIN")) {       
                // Sprawdz czy podano nazwe loginu    
                if (parametryPolecenia == null) {
                    Wyslij("Brak nazwy loginu");
                    cleanAndQuit();
                    return;
                }
                klientLogin = parametryPolecenia;
                if (! klienci.containsKey(klientLogin))
                    klienci.put( klientLogin, new KlientInfo(this) );
                else {
                    Wyslij ("Podany login juz jest zajety!");
                    try {
                        gniazdoPolaczenia.close();
                    } catch (IOException ex) { }
                }
                
            } else if (polecenie.equals("SEND")) {     
                //dzieli na login odbiorcy i komunikat
                String [] komenda = czesciWiadomosci[1].split(" ", 2);
                String komunikat = null;
                try {
                    otherLogin = komenda[0];
                    komunikat = komenda[1];
                } catch (RuntimeException e) {
                    Wyslij ("Za malo parametrow");
                    cleanAndQuit();
                    return;
                }
                
                //dodaj do kolejki komunikatow odbiorcy właściwy komunikat
                //(korzystając z pomocniczego słownika z jedną parą)
                //lub wyswietl od razu, jesli odbiorca ma ustawiona flage
                try {        
                    if (! klienci.get(otherLogin).autoReceive) {
                        map = new HashMap<String, String>();
                        map.put(klientLogin, komunikat);
                        //Dodaj do kolejki komunikatow czekajacych na odebranie
                        klienci.get(otherLogin).messageQueue.add(map);
                    } else {
                        //Nie dodawaj do kolejki, od razu wyswietl
                        klienci.get(otherLogin).asystent.Wyslij(klientLogin + ": "+ komunikat);
                    }
                }  catch (RuntimeException e) {
                    Wyslij ("Podany uzytkownik sie jeszcze nie zalogowal!");
                    cleanAndQuit();
                    return;
                }

            } else if (polecenie.equals("SENDALL")) {  
                //Pętla foreach iterująca po wartościach słownika zalogowanych
                //klientów
                for (KlientInfo receiver: klienci.values()) {
                    
                    //W zaleznosci, czy odbiorca chce odbierac wiadomosci
                    //automatycznie, wyswietl od razu lub dodaj do jego kolejki
                    //oczekujacych wiadomosci 
                    if (! receiver.autoReceive) {
                        //Dodaj do kolejki komunikatow odbiorcy komunikat
                        //korzystając z pomocniczego słownika z jedną parą 
                        // < loginNadawcy, wiadomosc >
                        map = new HashMap<String, String>();
                        map.put(klientLogin, czesciWiadomosci[1]);
                        receiver.messageQueue.add(map);
                    } else {
                        //nie dodawaj do kolejki, od razu wyswietl
                        receiver.asystent.Wyslij(klientLogin + ": "+ czesciWiadomosci[1]);
                    }
                }
                
            } else if (polecenie.equals("GET")) {
                //Pobiera wiadomosci tylko od wybranego uzytkownika, na poczatku sprawdza,
                //czy podano jego nazwę
                try {
                    otherLogin = czesciWiadomosci[1];
                } catch (RuntimeException e) {
                    Wyslij("Brak paremtru polecenia!");
                    cleanAndQuit();
                    return;
                }
                //Pomocnicza lista komunikatów, do której będą dodawane komunikaty przeznaczone do usunięcia
                //Usunięte zostaną zniorczo, po przejsciu całej pętli foreach 
                LinkedList <Map<String, String>> tempList = new LinkedList<Map<String, String>>();
                //Iteruj po komunikatach w postaci 1-elementowego słownika <nadawca, tresc>
                for (Map<String, String> message: klienci.get(klientLogin).messageQueue){
                    //sprawdz, czy aktualnie jest przetwarzany dobry komunikat
                    if (message.containsKey(otherLogin)) {
                        //jeśli tak, pobierz klucz i wartosc...
                        //...niestety w dość zawiły sposób
                        String key = message.keySet().iterator().next();
                        String value = message.get(key);
                        this.Wyslij(key+ ": " + value );
                        //dodaj do tymczasowej listy te komunikaty, ktore po przejsciu przez petle bedą usunięte
                        tempList.add(message);
                    }
                }
                if (tempList.isEmpty()) 
                    Wyslij("Nie otrzymales wiadomosci od uzytkownika "+otherLogin);
                else //usuń wszystkie dodane w powyższej pętli wiadomości 
                    klienci.get(klientLogin).messageQueue.removeAll(tempList);

            } else if (polecenie.equals("GETALL")) {   
                //Wyswietl cala kolejke komunikatow, po czym wyczysc kolejke
                
                //Pętla iterująca po komunikatach - 1-elementowe slowniki < nadawca, wiadomosc >
                for (Map<String, String> message: klienci.get(klientLogin).messageQueue) {
                    //mapa ma tylko jedną parę <klucz, wartość> więc można się bezpiecznie
                    //odwołać do jej pierwszego elementu
                    String key = message.keySet().iterator().next();
                    String value = message.get(key);
                    this.Wyslij(key+ ": " + value );
                }
                //na koniec wyczyść kolejke
                klienci.get(klientLogin).messageQueue.clear();

            } else if (polecenie.equals("AUTORECEIVE")) {  
                // Ustaw odpowiednio flagę automatycznego odbioru    
                klienci.get(klientLogin).autoReceive = 
                        ((czesciWiadomosci[1].equals("TRUE") || czesciWiadomosci[1].equals("ON")) 
                        ? true : false );
            } else if (polecenie.equals("LISTCLIENTS")) {
                String answer = "";
                //iteruj po wszystkich loginach
                for (String client: klienci.keySet()) {
                    answer += client;
                    answer += ";";
                }
                //usuń znak ';' konczący wiadomość
                answer = answer.substring(0, answer.length()-1);
                Wyslij(answer);
            } else if (polecenie.equals("EXIT")) {         
                //zakoncz, pozamykaj co sie da
                cleanAndQuit();
            
            } else {
                this.Wyslij("Nieznane polecenie");
            }
        }

        /** Pozamykaj połączenia, rozłącz */
        private void cleanAndQuit() {
            try {
                klienci.remove(klientLogin);
                daneOdbierane.close();
                daneWysylane.close();
                gniazdoPolaczenia.close();
            } catch (IOException e) {

            }
        }
    }
}