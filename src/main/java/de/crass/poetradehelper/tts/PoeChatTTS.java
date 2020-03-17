package de.crass.poetradehelper.tts;

import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
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

//IDEA: Notify on tendency change - Check after parsing every deal for tendency check? / Store tendency in deal?
public class PoeChatTTS {
    private static final String logsFolder = "logs";
    private static final String bestVoiceEver = "ScanSoft Daniel_Full_22kHz";

    private Path path;
    private final Listener listener;

    // 0 to 100
    private int volume = 100;
    // -10 to 10
    private int speed = 0;

    private String voice;

    private boolean randomizeMessages = true;
    private boolean readAFK = true;
    private boolean readTradeRequests = true;
    private boolean readCurrencyRequests = true;
    private boolean readChatMessages = false;
    private boolean readShoutout = true;
    private boolean readShoutoutMessage = true;

    private final String[] startPhrases = {
            "guess what", "check that out", "wait a second", "hold on", "what the fuck", "did you see", "listen",
            "DUDE", "nice", "hello there", "holy cow", "wonderful", "watch out"};
    private final String[] names = {
            "an exile", "someone", "some guy", "a player", "some dude", "this cheeky scrub lord", "this wannabe rockefeller"};
    private final String[] attributes = {
            "with too much cash", "here", "over there", "being generous"};
    private final String[] endPhrases = {
            "congratulations", "good for you", "what a noob", "how fortunate", "sweet", "savage", "radical", "groovay",
            "awesome", "what you gonna do?", "you are welcome"};

    private String[] knownNames = {
            "that same guy from before,", "someone who could not get enough" ,"some guy who came back"};

    private String[] wantsBuyText = {
            "wants to buy your", "wants your", "wants to trade your"};

//    private String[] shoutOuts = {
//            "this chat message seems interesting!", "something of interest got mentioned in the chat!"};
    private String[] shoutOuts = {
            "mentioned something of interest in the chat", "says something interesting in the chat"};

    private String[] testPhrases = {
            "One", "Check", "Boom", "Dude"};

    private Thread watchDogThread;
    private Runnable watchDog;
    private TextField wordIncludeTextField;
    private TextField wordExcludeTextField;

    private List<String> playersMet = new LinkedList<>();
    private Process speechProcess;
    private boolean useLogTail = true;
    private boolean isRunning = false;

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

    private void processNewLine(String newLine) {
        if (readAFK && newLine.contains("AFK mode is now ON.")) {
            try {
                textToSpeech("You just went AFK!");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        Pattern sayPattern = Pattern.compile("].+? (.+?): (.+)");
        Pattern currencyPattern = Pattern.compile("] @From (.+?): .+your \\d+ (.+) for my \\d+ (.+) i");
        Pattern tradePattern = Pattern.compile("] @From (.+?):.+ buy your (.+) listed for \\d+ (.+) in");
        Matcher matcher = currencyPattern.matcher(newLine);

        if (readCurrencyRequests && matcher.find() || readTradeRequests && (matcher = tradePattern.matcher(newLine)).find()) {
            String playerName = matcher.group(1);
            boolean playerKnown = false;
            if (playersMet.contains(playerName)) {
                playerKnown = true;
            }else{
                playersMet.add(playerName);
            }

            // Currency buy request
            String buyItem = matcher.group(2);
            String sellItem = matcher.group(3);

            String ttsMessage = getTradeMessage(buyItem, sellItem, playerKnown);

            try {
                textToSpeech(ttsMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ((readChatMessages || (readShoutout && wordIncludeTextField != null && !wordIncludeTextField.getText().isEmpty()))
                && ((matcher = sayPattern.matcher(newLine)).find())) {

            String name = readableName(matcher.group(1));
            // Name is a NPC, cancel.
            if (name == null) {
                return;
            }
            String msg = matcher.group(2);
            String[] words = msg.split("\\s+");

            if (readShoutout && wordIncludeTextField != null && !wordIncludeTextField.getText().isEmpty()) {
                String[] includeWords = wordIncludeTextField.getText().split(",");
                String[] excludeWords = wordExcludeTextField.getText().split(",");
                boolean included = false;
                boolean excluded = false;
                for (String wordInMsg : words) {
                    // If included found, skip included search
                    if (!included) {
                        for (String word : includeWords) {
                            if (word.equalsIgnoreCase(wordInMsg)) {
                                included = true;
                                break;
                            }
                        }
                        // if word was included, skip exclusion search for this i
                        if (included) {
                            continue;
                        }
                    }

                    for (String exWord : excludeWords) {
                        if (wordInMsg.equalsIgnoreCase(exWord)) {
                            excluded = true;
                            break;
                        }
                    }
                    // if word was excluded, cancel search
                    if (excluded) {
                        break;
                    }
                }

                if (included && !excluded) {
                    readShoutoutMessage = newLine.length() < 125;
                    if(readShoutoutMessage) {
                        try {
                            textToSpeech(name + " says " + convertSlangToSpoken(words));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        String speech = randomizeMessages ? getRandomStartPhrase()+ name + getRandomString(shoutOuts) : shoutOuts[0];
                        try {
                            textToSpeech(speech);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                }
            }
            if (readChatMessages) {
                String verb = "says";

                // Check if sent by player
                if (newLine.contains("@To")) {
                    name = "you";
                    verb = "say";
                }
                msg = convertSlangToSpoken(words);

                try {
                    textToSpeech(name + ' ' + verb + ' ' + msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getTradeMessage(String buyItem, String sellItem, boolean playerKnown) {
        String ttsMessage;
        if (randomizeMessages) {
            String name = getRandomName();
            if (playerKnown) {
                name = getRandomString(knownNames) + ' ';
            }
            ttsMessage = getRandomStartPhrase() + name + getRandomAttribute() + getRandomString(wantsBuyText) + " " + buyItem + " for his " + sellItem + getRandomEndPhrase();
        } else {
            String name = names[0] + ' ';
            if (playerKnown) {
                name = knownNames[0] + ' ';
            }
            // Someone wants to buy your alchemy for exalted
            ttsMessage = name + wantsBuyText[0] + " " + buyItem + " for " + sellItem;
        }
        return ttsMessage;
    }

    public void setVoice(String voice) {
        if(voice != null) {
            PropertyManager.getInstance().setProp(PropertyManager.VOICE_SPEAKER, voice);
        }
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

        // If found and no voice set yet, set bestVoiceEver as voice.
        if(voice == null && result.contains(bestVoiceEver)){
            setVoice(bestVoiceEver);
        } // Else set first found. Remove this for continuous checking for bestVoiceEver...
        else if(voice == null && !result.isEmpty()){
            setVoice(result.get(0));
        }

        return result;
    }

    public void startTTS() {
        useLogTail = !Boolean.parseBoolean(PropertyManager.getInstance().getProp("tts_watchdog", "false"));
        if(useLogTail){
            path = path.resolve("Client.txt");
            watchDog = new LogTailer(path.toFile(), true, new FileListener() {
                @Override
                public void onFileChanged(File path, boolean newFile) {

                }

                @Override
                public void onNewLine(File file, String newLine) {
                    if (file.getName().contains("Worker")) {
                        return;
                    }
                    processNewLine(newLine);
                }

                @Override
                public void onShutdown() {
                    if (listener != null) {
                        listener.onShutDown();
                    }
                }
            });
        } else {
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
        }
        watchDogThread = new Thread(watchDog, "logWatcher");
        watchDogThread.start();
        isRunning = true;
        LogManager.getInstance().log(getClass(), "TTS Watchdog started.");

        // Possible bugfix for starting the watchdog....
//        FileInputStream is = null;
//        try {
//            File logFile = new File(path+"/Client.txt");
//            is = new FileInputStream(logFile);
//            int read;
//            while(is.available()>0){
//                is.read();
//            };
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if(is != null) {
//                try {
//                    is.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    public void stopTTS() {
        if(speechProcess != null){
            speechProcess.destroy();
            speechProcess = null;
        }
        isRunning = false;
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
        String phrase = getRandomString(startPhrases, 20);

        if (phrase == null) {
            return "";
        }
        while (phrase.equals(lastStartPhrase)) {
            phrase = getRandomString(startPhrases);
        }

        lastStartPhrase = phrase;
        return phrase + ", ";

    }

    private String lastAttribute = "";

    private String getRandomAttribute() {
        String phrase = getRandomString(attributes, 10);

        if (phrase == null) {
            return "";
        }
        while (phrase.equals(lastAttribute)) {
            phrase = getRandomString(attributes);
        }

        lastAttribute = phrase;
        return phrase + ' ';

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
        return phrase + ' ';
    }

    private String readableName(String name) {
        // Check if NPC
        if (name.contains(" ")) {
            return null;
        }
        name = name.replace('_', ' ');
        for (int i = 0; i < name.length(); i++) {
            if (!Character.UnicodeBlock.of(name.charAt(i)).equals(Character.UnicodeBlock.BASIC_LATIN)) {
                return names[0];
            }
        }
        return name.replace('_', ' ');
    }

    private String convertSlangToSpoken(String[] words) {
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
        textToSpeech(text, false);
    }

    public void textToSpeech(String text, boolean interrupt) throws IOException {
        LogManager.getInstance().log(getClass(), '\"' + text + '\"');

        if(interrupt && speechProcess != null && speechProcess.isAlive()){
            speechProcess.destroy();
        }

        String voiceParam = "";
        if (voice != null) {
            voiceParam = " -n " + voice;
        }
        speechProcess = Runtime.getRuntime().exec("balcon -t \"" + text + "\"" + voiceParam + " -v " + volume + " -s " + speed);

        // If not waiting here the order gets messed up
        if(!interrupt) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
        return watchDog != null && isRunning;
    }

    public String getVoice() {
        return voice;
    }

    public void testSpeech() {
        String test = getRandomString(endPhrases, 8);
        if (test == null) {
            test = getRandomString(testPhrases);
        }

        try {
            textToSpeech(test, true);
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

    public void randomTradeMessage() {
        CurrencyID buy = CurrencyID.getRandom();
        CurrencyID sell = CurrencyID.getRandom();
        boolean known = Math.random() > 0.8;
        try {
            textToSpeech(getTradeMessage(buy.toString(), sell.toString(), known), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        GL("good luck"),
        LF("looking for"),
        LFM("looking for member"),
        WTB("want to buy"),
        HIDEOUT("hide out");

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
        if (!path.endsWith(logsFolder)) {
            if (!path.endsWith("\\")) {
                path += "\\";
            }
            path += logsFolder;
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
