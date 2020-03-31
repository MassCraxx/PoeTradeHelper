package de.crass.poetradehelper.tts;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.Main;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.ParseConfig;
import de.crass.poetradehelper.model.PatternOutput;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//IDEA: Notify on tendency change - Check after parsing every deal for tendency check? / Store tendency in deal?
public class PoeChatTTS implements FileListener{
    private static final String bestVoiceEver = "ScanSoft Daniel_Full_22kHz";

    private String[] knownNames = {
            "that same guy from before,;someone who could not get enough;some guy who came back"};

    private String[] shoutOuts = {
            "mentioned something of interest in the chat", "says something interesting in the chat"};

    private String[] testPhrases = {
            "One", "Check", "Boom", "Dude"};

    // Config
    private ParseConfig parseConfig;

    // Properties
    // 0 to 100
    private int volume = 100;
    // -10 to 10
    private int speed = 0;
    private String voice;

    // RemoveMe?
    private TextField wordIncludeTextField;
    private TextField wordExcludeTextField;
    private boolean useIncludeExclude = false;

    // Threads and Processes
    private Process speechProcess;
    private LogTailer logTailer;

    private ExecutorService executorService;

    private boolean isRunning = false;
    private File configFile = new File("./ttsconfig.json");
    private Listener listener;

    public PoeChatTTS(Listener listener) {
        this.listener = listener;

        init();
    }

    private void init() {
        PropertyManager proMan = PropertyManager.getInstance();
        setVolume(proMan.getVoiceVolume());
        setVoice(proMan.getProp(PropertyManager.VOICE_SPEAKER, null));
    }

    public void setParseConfig(ParseConfig parseConfig) {
        if (parseConfig == null) {
            return;
        }

//        boolean wasRunning = false;
//        if(isRunning){
//            wasRunning = true;
//            stopTTS();
//        }

        this.parseConfig = parseConfig;

//        if(wasRunning) {
//            startTTS();
//        }
    }

    private void processNewLine(String newLine) {
        LogManager.getInstance().log(getClass(), "Processing: " + newLine);

        if (parseConfig == null) {
            return;
        }

        if (parseConfig.getPatternToOutput().isEmpty()) {
            try {
                textToSpeech(newLine);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (PatternOutput patternOutput : parseConfig.getPatternToOutput()) {
            // Skip line if pattern is inactive
            if (!patternOutput.isActive()) {
                continue;
            }

            // Check pattern
            Pattern pattern = Pattern.compile(patternOutput.getPattern());
            Matcher m = pattern.matcher(newLine);

            if (m.find()) {
                // Only parse if probability given
                int probability = patternOutput.getProbability();
                if (probability < 100) {
                    int random = ThreadLocalRandom.current().nextInt(0, 100);
                    if (random < probability) {
                        LogManager.getInstance().log(getClass(), "Ignored (" + probability + "% probability)");
                        continue;
                    }
                }

                String[] output = patternOutput.getOutput().split(";");
                String voiceOutput = getRandomString(output);

                StringBuilder log = new StringBuilder();
                for (int g = 0; g <= m.groupCount(); g++) {
                    voiceOutput = voiceOutput.replace("(" + g + ")", m.group(g));

                    log.append(g)
                            .append(":(")
                            .append(m.group(g))
                            .append(") ");
                }
                LogManager.getInstance().log(getClass(), log.toString());

                String[] words = voiceOutput.split("\\s+");
                if (useIncludeExclude) {
                    if (wordIncludeTextField != null && !wordIncludeTextField.getText().isEmpty()) {
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

                        if (!included || excluded) {
                            return;
                        }
                    }
                }
                try {
                    textToSpeech(replacePlaceholders(words));
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String replacePlaceholders(String[] words) {
        boolean addSpace = false;
        StringBuilder sb = new StringBuilder();
        for (String s : words) {
            if (addSpace) {
                sb.append(' ');
            } else {
                addSpace = true;
            }
            String word = s;

            if (word.startsWith("%")) {
                int probability = 100;
                String[] placeholderConfig = word.split("%");
                if (placeholderConfig.length > 2) {
                    if (Character.isDigit(placeholderConfig[1].charAt(0))) {
                        probability = Integer.parseInt(placeholderConfig[1]);
                    } else {
                        LogManager.getInstance().log(getClass(), "Could not parse probability in " + word);
                    }
                    word = placeholderConfig[2];
                } else {
                    word = placeholderConfig[1];
                }

                StringBuilder replacementBuilder = new StringBuilder();
                for (String replacementKey : word.split("&")) {
                    String replacementPart = parseConfig.getPlaceholders().get(replacementKey);
                    if (replacementPart != null) {
                        if (replacementBuilder.length() > 0) {
                            replacementBuilder.append(";");
                        }
                        replacementBuilder.append(replacementPart);
                    }
                }
                String replacement = replacementBuilder.toString();
                if (!replacement.isEmpty()) {
                    replacement = getRandomString(replacement.split(";"), probability);
                    LogManager.getInstance().log(getClass(), "Replacement for " + word + " - " + replacement + " (" + probability + "%)");
                    if (replacement != null) {
                        word = replacePlaceholders(replacement.split("\\s+"));
                        sb.append(word);
                        continue;
                    }
                }
                addSpace = false;
            } else {
                word = convertSlangToSpoken(word);
                sb.append(word);
            }
        }
        return sb.toString();
    }

    public void setVoice(String voice) {
        if (voice != null) {
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
            // Normal if balcon not found
//            LogManager.getInstance().log(getClass(), "Exception during checking voice support. " + e);
            return null;
        }

        // If found and no voice set yet, set bestVoiceEver as voice.
        if (voice == null && result.contains(bestVoiceEver)) {
            setVoice(bestVoiceEver);
        } // Else set first found. Remove this for continuous checking for bestVoiceEver...
        else if (voice == null && !result.isEmpty()) {
            setVoice(result.get(0));
        }

        return result;
    }

    public void startTTS() {
        if (!loadConfig()) {
            LogManager.getInstance().log(getClass(), "ERROR: StartTTS failed! No Config set.");
            return;
        } else if (parseConfig.getPatternToOutput().isEmpty()) {
            LogManager.getInstance().log(getClass(), "Config contains no pattern, everything will be red.");
        }

        String dir = PropertyManager.getInstance().getPathOfExilePath();
        if(dir.charAt(dir.length() - 1) != '/'){
            dir += "/";
        }

        File file = new File(dir + "\\logs\\Client.txt");
        if (file.exists()) {
            startLogTail(file);

            isRunning = true;
            LogManager.getInstance().log(getClass(), "TTS Watchdog started.");
        } else {
            LogManager.getInstance().log(getClass(),"Check your PoE Path! Log file not found. " + file.getAbsolutePath() + " does not exist.");
            onShutdown();
        }

    }

    public void stopTTS() {
        if (speechProcess != null) {
            speechProcess.destroy();
            speechProcess = null;
        }

        if (logTailer != null) {
            logTailer.stopRunning();
            logTailer = null;
        }
    }

    public void shutdown() {
        stopTTS();

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private String getRandomString(String[] list) {
        return getRandomString(list, 100);
    }

    private String getRandomString(String[] list, int probability) {
        int random = ThreadLocalRandom.current().nextInt(0, 100);
        if (random < probability) {
            String phrase;
            int randomNum = ThreadLocalRandom.current().nextInt(0, list.length);
            phrase = list[randomNum];
            return phrase;
        }
        return null;
    }

    private String convertSlangToSpoken(String[] words) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(String word : words){
            if(!first){
                result.append(" ");
            }
            first = false;
            result.append(convertSlangToSpoken(word));
        }
        return result.toString();
    }

    private String convertSlangToSpoken(String word) {
        for (InternetSlang slang : InternetSlang.values()) {
            if (word.toUpperCase().startsWith(slang.name())) {
                // Only if word is not longer than one character more (punctuation)
                if (word.length() <= slang.name().length() + 1) {
                    word = word.toUpperCase();
                    word = word.replace(slang.name(), slang.getSpokenTerm());
                }
            }
        }
        return word;
    }

    private void textToSpeech(String text) throws IOException {
        textToSpeech(text, false);
    }

    public void textToSpeech(String text, boolean interrupt) throws IOException {
        LogManager.getInstance().log(getClass(), "Voice: \"" + text + '\"');

        if (interrupt && speechProcess != null && speechProcess.isAlive()) {
            speechProcess.destroy();
        }

        String voiceParam = "";
        if (voice != null) {
            voiceParam = " -n " + voice;
        }
        speechProcess = Runtime.getRuntime().exec("balcon -t \"" + text + "\"" + voiceParam + " -v " + volume + " -s " + speed);

        // If not waiting here the order gets messed up
        if (!interrupt) {
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

    public boolean isActive() {
        return isRunning;
    }

    public String getVoice() {
        return voice;
    }

    public void testSpeech() {
        String test;
        if (parseConfig == null || (test = parseConfig.getPlaceholders().get("test")) == null) {
            test = getRandomString(testPhrases);
        } else {
            test = replacePlaceholders(getRandomString(test.split(";")).split("\\s+"));
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

    public ParseConfig getParseConfig() {
        return parseConfig;
    }

    public void notifyBadTendency() throws IOException {
        String badTendencyString = parseConfig.getPlaceholders().get("bad_tendency");
        if(badTendencyString == null){
            badTendencyString = getRandomString(new String[]{"Update your offers"});
        }

        textToSpeech(badTendencyString);
    }

    public enum InternetSlang {

        SRY("sorry"),
        STFU("shut the fuck up"),
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

    private void startLogTail(File file) {
        if (logTailer != null) {
            logTailer.stopRunning();
        }
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(4);
        }
        logTailer = new LogTailer(file, true, this);
        executorService.execute(logTailer);
    }

    @Override
    public void onFileChanged(File file, boolean newFile) {

    }

    public boolean loadConfig() {
        File file = configFile;
        if (file.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ParseConfig config = mapper.readValue(file, ParseConfig.class);
                LogManager.getInstance().log(getClass(), "Config successfully loaded.");
                setParseConfig(config);
                return true;
            } catch (JsonMappingException j) {
                LogManager.getInstance().log(getClass(), "Config corrupted! " + j.getMessage());
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Error while loading config.");
                e.printStackTrace();
            }
        } else {
            try {
                URL url = Main.class.getResource("default_ttsconfig.json");
                ObjectMapper mapper = new ObjectMapper();
                ParseConfig config = mapper.readValue(url, ParseConfig.class);
                LogManager.getInstance().log(getClass(), "Using default config.");

                storeConfig(config);
                setParseConfig(config);
                return true;
            } catch (JsonMappingException j) {
                LogManager.getInstance().log(getClass(), "Config corrupted! " + j.getMessage());
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Error while loading config.");
                e.printStackTrace();
            }
        }
        LogManager.getInstance().log(getClass(), "Loading config failed.");
        return false;
    }

    private void storeConfig(ParseConfig config) {
        LogManager.getInstance().log(getClass(), "Storing config file in " + configFile);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        ObjectWriter writer = mapper.writer();
        try {
            writer.writeValue(configFile, config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewLine(File file, String newLine) {
        processNewLine(newLine);
    }

    @Override
    public void onShutdown() {
        isRunning = false;
        if (listener != null) {
            listener.onShutDown();
        }
    }

    public interface Listener {
        void onShutDown();
    }
}
