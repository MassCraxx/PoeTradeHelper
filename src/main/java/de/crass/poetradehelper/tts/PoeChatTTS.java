package de.crass.poetradehelper.tts;

import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PoeChatTTS {
    final private String logsFolder = "logs";

    private Path path;
    private final Listener listener;

    private int volume = 100;
    private int speed = 0;
    private String voice;

    private boolean randomizeMessages = true;

    private boolean readAFK = true;
    private boolean readTradeRequests = true;
    private boolean readCurrencyRequests = true;
    private boolean readChatMessages = false;

    private final String[] startPhrases = {"guess what", "check that out", "wait a second", "hold on", "what the fuck", "did you see", "listen"};
    private final String[] names = {"someone", "some dude", "this cheeky scrub lord", "some guy with too much cash", "an exile"};
    private final String[] endPhrases = {"congratulations", "good for you", "what a noob", "how fortunate"};
    private String[] testPhrases = {"One", "Check", "Boom"};

    private Thread watchDogThread;
    private WatchDog watchDog;
    private TextField wordIncludeTextField;
    private TextField wordExcludeTextField;

    public PoeChatTTS(Listener listener) {
        this(PropertyManager.getInstance().getPathOfExilePath(), listener);
    }

    private PoeChatTTS(String path, Listener listener) {
        setPath(path);
        this.listener = listener;

        init();
    }

    private void init() {
        PropertyManager proMan = PropertyManager.getInstance();
        setVolume(proMan.getVoiceVolume());
        setVoice(proMan.getProp(PropertyManager.VOICE_SPEAKER, null));
        setReadTradeRequests(Boolean.parseBoolean(proMan.getProp(PropertyManager.VOICE_TRADE, "true")));
        setReadCurrencyRequests(Boolean.parseBoolean(proMan.getProp(PropertyManager.VOICE_CURRENCY, "true")));
        setReadChatMessages(Boolean.parseBoolean(proMan.getProp(PropertyManager.VOICE_CHAT, "false")));
        setReadAFK(Boolean.parseBoolean(proMan.getProp(PropertyManager.VOICE_AFK, "true")));
        setRandomizeMessages(Boolean.parseBoolean(proMan.getProp(PropertyManager.VOICE_RANDOMIZE, "true")));
    }

    private void processNewLine(String newLine){
        if (readAFK && newLine.contains("AFK mode is now ON.")) {
            try {
                textToSpeech("You just went AFK!");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        else if (wordIncludeTextField !=null && !wordIncludeTextField.getText().isEmpty()) {
            String[] includeWords = wordIncludeTextField.getText().split(",");
            String[] excludeWords = wordExcludeTextField.getText().split(",");
            boolean excluded = false;
            for(String exWord : excludeWords){
                if(newLine.contains(exWord)){
                    excluded = true;
                    break;
                }
            }
            if(!excluded) {
                for (String word : includeWords) {
                    if (newLine.contains(word)) {
                        try {
                            textToSpeech("This chat message seems interesting!");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                }
            }
        }

        Pattern sayPattern = Pattern.compile("].+? (.+?): (.+)");
        Pattern currencyPattern = Pattern.compile("] @From (.+?): .+your \\d+ (.+) for my \\d+ (.+) i");
        Pattern tradePattern = Pattern.compile("] @From (.+?):.+ buy your (.+) listed for \\d+ (.+) in");
        Matcher matcher = currencyPattern.matcher(newLine);

        if (readCurrencyRequests && matcher.find() || readTradeRequests && (matcher = tradePattern.matcher(newLine)).find()) {
            // Currency buy request
            String buyCurrency = matcher.group(2);
            String sellCurrency = matcher.group(3);

            String ttsMessage;
            if (randomizeMessages) {
                ttsMessage = getRandomStartPhrase() + getRandomName() + " wants to buy your " + buyCurrency + " for " + sellCurrency + getRandomEndPhrase();
            } else {
                ttsMessage = "Someone wants to buy your " + buyCurrency + " for " + sellCurrency;
            }
            try {
                textToSpeech(ttsMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (readChatMessages && ((matcher = sayPattern.matcher(newLine)).find())) {
            String name = readableName(matcher.group(1));
            // Name is a NPC, cancel.
            if (name == null) {
                return;
            }
            String msg = matcher.group(2);
            String verb = "says";

            // Check if sent by player
            if (newLine.contains("@To")) {
                name = "you";
                verb = "say";
            }
            msg = convertSlangToSpoken(msg);

            try {
                textToSpeech(name + ' ' + verb + ' ' + msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public void setVolume(int volume) {
        if (volume > 100) {
            volume = 100;
        } else if (volume < 0) {
            volume = 0;
        }
        this.volume = volume;
    }

    public void setSpeed(int speed) {
        if (speed < -10) {
            speed = -10;
        } else if (speed > 10) {
            speed = 10;
        }
        this.speed = speed;
    }

    public boolean isVoiceSupported(String preferredVoice) {
        try {
            return executeCommand("balcon -l").contains(preferredVoice);
        } catch (InterruptedException | IOException e) {
//            LogManager.getInstance().log(getClass(), "Exception during checking voice support. " + e);
        }
        return false;
    }

    public List<String> getSupportedVoices() {
        List<String> result = new LinkedList<>();
        try {
            String[] voices = executeCommand("balcon -l").split("\n");

            for (int i = 2; i < voices.length; i++) {
                result.add(voices[i].trim());
            }
        } catch (InterruptedException | IOException e) {
//            LogManager.getInstance().log(getClass(), "Exception during checking voice support. " + e);
            return null;
        }
        return result;
    }

    public void startTTS() {
        watchDog = new WatchDog(path, new WatchDog.Listener() {
            @Override
            public void onFileChanged(File path) {

            }

            @Override
            public void onNewLine(File file, String newLine) {
                if (file.getName().contains("Worker")) {
                    return;
                }
                processNewLine(newLine);
            }

            @Override
            public void onShutDown() {
                if (listener != null) {
                    listener.onShutDown();
                }
            }
        });

        watchDogThread = new Thread(watchDog, "logWatcher");
        watchDogThread.start();
        LogManager.getInstance().log(getClass(), "TTS Watchdog started.");
    }

    public void stopTTS() {
        watchDog.stop();
        watchDogThread.interrupt();
        watchDog = null;
        watchDogThread = null;
    }

    private String getRandomString(String[] list) {
        return getRandomString(list, 100);
    }

    private String getRandomString(String[] list, int probability) {
        int random = ThreadLocalRandom.current().nextInt(0, 100);
        if (random < probability) {
            int randomNum;
            String phrase;
            randomNum = ThreadLocalRandom.current().nextInt(0, list.length);
            phrase = list[randomNum];
            return phrase;
        }
        return null;
    }

    private String lastStartPhrase = "";

    private String getRandomStartPhrase() {
        String phrase = getRandomString(startPhrases, 10);

        if (phrase == null) {
            return "";
        }
        while (phrase.equals(lastStartPhrase)) {
            phrase = getRandomString(startPhrases);
        }

        lastStartPhrase = phrase;
        return phrase + ", ";

    }

    private String lastEndPhrase;

    private String getRandomEndPhrase() {
        String phrase = getRandomString(endPhrases, 10);

        if (phrase == null) {
            return "";
        }

        while (phrase.equals(lastEndPhrase)) {
            phrase = getRandomString(endPhrases);
        }

        lastEndPhrase = phrase;
        return ", " + phrase;
    }

    private String lastName;

    private String getRandomName() {

        String phrase;
        do {
            phrase = getRandomString(names);
        } while (phrase.equals(lastName));

        lastName = phrase;
        return phrase;
    }

    private String readableName(String name) {
        // Check if NPC
        if (name.contains(" ")) {
            return null;
        }
        name = name.replace("_", " ");
        for (int i = 0; i < name.length(); i++) {
            if (!Character.UnicodeBlock.of(name.charAt(i)).equals(Character.UnicodeBlock.BASIC_LATIN)) {
                return "Someone";
            }
        }
        return name.replace("_", " ");
    }

    private String convertSlangToSpoken(String msg) {
        msg = msg.replace("_", " ");

        String[] words = msg.split("\\s+");

        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            for (InternetSlang slang : InternetSlang.values()) {
                if (word.toUpperCase().startsWith(slang.name())) {
                    // Only if word is not longer than one character more (punctuation)
                    if (word.length() <= slang.name().length() + 1) {
                        word = word.toUpperCase();
                        word = word.replace(slang.name(), slang.getSpokenTerm());
                    }
                }
            }
            sb.append(word);
            sb.append(' ');
        }

        return sb.toString();
    }

    private void textToSpeech(String text) throws IOException {
        LogManager.getInstance().log(getClass(), '\"' + text + '\"');

        String voiceParam = "";
        if (voice != null) {
            voiceParam = " -n " + voice;
        }
        Runtime.getRuntime().exec("balcon -t \"" + text + "\"" + voiceParam + " -v " + volume + " -s " + speed);

        // If not waiting here the order gets messed up
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String executeCommand(String command) throws InterruptedException, IOException {
        StringBuilder output = new StringBuilder();

        Process p;
        p = Runtime.getRuntime().exec(command);
        p.waitFor();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        reader.close();

        return output.toString();
    }

    public void setRandomizeMessages(boolean randomizeMessages) {
        this.randomizeMessages = randomizeMessages;
    }

    public void setReadChatMessages(boolean readChatMessages) {
        this.readChatMessages = readChatMessages;
    }

    public void setReadAFK(boolean readAFK) {
        this.readAFK = readAFK;
    }

    public void setReadTradeRequests(boolean readTradeRequests) {
        this.readTradeRequests = readTradeRequests;
    }

    public void setReadCurrencyRequests(boolean readCurrencyRequests) {
        this.readCurrencyRequests = readCurrencyRequests;
    }

    public boolean isActive() {
        return watchDog != null && watchDog.isRunning();
    }

    public String getVoice() {
        return voice;
    }

    public void testSpeech() {
        String text = getRandomString(endPhrases, 8);
        if(text == null){
            text = getRandomString(testPhrases);
        }

        try {
            textToSpeech(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setWordIncludeTextField(TextField wordIncludeTextField) {
        this.wordIncludeTextField = wordIncludeTextField;
    }

    private TextField getWordIncludeTextField() {
        return wordIncludeTextField;
    }

    public void setWordExcludeTextField(TextField wordExcludeTextField) {
        this.wordExcludeTextField = wordExcludeTextField;
    }

    private TextField getWordExcludeTextField() {
        return wordExcludeTextField;
    }

    public enum InternetSlang {

        SRY("sorry"),
        NVM("nevermind"),
        WTF("what the fuck"),
        GTFO("get the fuck out"),
        SRSLY("seriously"),
        FU("fuck you"),
        TY("thank you"),
        THX("thanks"),
        T4T("thanks for trade"),
        N1("nice one"),
        GJ("good job"),
        WP("well played"),
        HF("have fun"),
        GL("good luck");

        String spokenTerm;

        InternetSlang(String spokenTerm) {
            this.spokenTerm = spokenTerm;
        }

        public String getSpokenTerm() {
            return spokenTerm;
        }
    }

    public boolean isReadAFK() {
        return readAFK;
    }

    public boolean isReadTradeRequests() {
        return readTradeRequests;
    }

    public boolean isReadCurrencyRequests() {
        return readCurrencyRequests;
    }

    public boolean isReadChatMessages() {
        return readChatMessages;
    }

    public void setPath(String path) {
        if(!path.endsWith(logsFolder)){
            if (!path.endsWith("\\")) {
                path += "\\";
            }
            path +=logsFolder;
        }

        this.path = Paths.get(path);
    }

    public boolean isRandomizeMessages() {
        return randomizeMessages;
    }

    public interface Listener {
        void onShutDown();
    }
}
