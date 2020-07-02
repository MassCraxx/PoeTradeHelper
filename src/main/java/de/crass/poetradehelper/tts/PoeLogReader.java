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
import de.crass.poetradehelper.ui.OverlayManager;
import javafx.scene.control.TextField;

import java.awt.*;
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
public class PoeLogReader implements FileListener {
    private static final String bestVoiceEver = "ScanSoft Daniel_Full_22kHz";

    private String[] knownNames = {
            "that same guy from before,;someone who could not get enough;some guy who came back"};

    private String[] shoutOuts = {
            "(1) got mentioned in the chat!",
            "Someone in the chat is talking about (1)"};

    private String[] testPhrases = {
            "One", "Check", "Boom", "Dude"};

    private String[] inTestMessages = {
            "2020/04/28 16:11:55 10434343 acf [INFO Client 11664] @From <=‡----> FREE_Space: Hi, I would like to buy your Dusk Creed Small Cluster Jewel listed for 15 chaos in Delirium (stash tab \"xc\"; position: left 5, top 4)",
            "2020/04/15 20:48:45 31274312 acf [INFO Client 13456] @From Legion_undead: Hi, I'd like to buy your 1 Exalted Orb for my 740 Cartographer's Chisel in Delirium.",
    };
    private String[] outTestMessages = {
            "2020/04/14 00:28:54 32415359 acf [INFO Client 1944] @To Critikills: Hi, I would like to buy your level 19 0% Multistrike Support listed for 5 chaos in Delirium (stash tab \"cheap 2\"; position: left 2, top 5)",
            "2020/07/01 23:12:00 24271359 b5c [INFO Client 3916] @To Dowlost_NW_SVD: Hi, I would like to buy your Ryslatha's Coil Studded Belt listed for 5.5 exalted in Harvest (stash tab \"S2\"; position: left 11, top 12)"
    };

    // Config
    private ParseConfig ttsParseConfig;

    // Properties
    // 0 to 100
    private int volume = 100;
    // -10 to 10
    private int speed = 0;
    private String voice;

    // RemoveMe?
    private TextField wordIncludeTextField;
    private TextField wordExcludeTextField;
    private boolean useIncludeExclude = true;

    // Threads and Processes
    private List<Process> speechProcesses = new LinkedList<>();
    private LogTailer logTailer;

    private ExecutorService executorService;

    private boolean isRunning = false;
    private boolean useTTS;
    private boolean showOverlay = true;

    private File configFile = new File("./ttsconfig.json");
    private Listener listener;
    private Pattern allowedChars = Pattern.compile(PropertyManager.getInstance().getProp("voice_allowed_chars", "[A-Za-z0-9%'.,!?()+-/&=$ ]+"));

    private Pattern tradePattern = Pattern.compile("@.+ (.+):.+your (.+) listed for (\\d.+) i.+\"(.+)\".+left (\\d+).+top (\\d+).+");
    private Pattern currencyPattern = Pattern.compile("@.+ (.+):.+r (\\d+ .+) for my (\\d+ .+) i.+");
    private Pattern unpricedTradePattern = Pattern.compile("@.+ (.+):.+your (.+) i.+\"(.+)\".+left (\\d+).+top (\\d+).+");

    private boolean notifyCurrencyRequests = PropertyManager.getInstance().getBooleanProp("notify_currency", true);
    private boolean notifyTradeRequests = PropertyManager.getInstance().getBooleanProp("notify_trade", true);

    public PoeLogReader(Listener listener) {
        this.listener = listener;

        init();
    }

    private void init() {
        PropertyManager proMan = PropertyManager.getInstance();
        setVolume(proMan.getVoiceVolume());
        setVoice(proMan.getProp(PropertyManager.VOICE_SPEAKER, null));
    }

    public void setTTSParseConfig(ParseConfig parseConfig) {
        if (parseConfig == null) {
            return;
        }

        this.ttsParseConfig = parseConfig;
    }

    private void processNewLine(String newLine) {
//        LogManager.getInstance().log(getClass(), "Processing: " + newLine);

        // Process trade for overlay
        Matcher matcher;

        boolean in = newLine.contains("@From");
        boolean out = newLine.contains("@To");
        if ((in && PropertyManager.getInstance().getBooleanProp(PropertyManager.NOTIFY_INCOMING, true) ||
                (out && PropertyManager.getInstance().getBooleanProp(PropertyManager.NOTIFY_OUTGOING, true)))) {
            if (notifyTradeRequests && (matcher = tradePattern.matcher(newLine)).find() ||
                    notifyCurrencyRequests && (matcher = currencyPattern.matcher(newLine)).find() ||
                    notifyTradeRequests && (matcher = unpricedTradePattern.matcher(newLine)).find()) {
                int x = -1;
                int y = -1;
                String stashTab = "";
                if (matcher.groupCount() >= 4) {
                    stashTab = matcher.group(4);
                    if (matcher.groupCount() >= 6) {
                        x = Integer.parseInt(matcher.group(5));
                        y = Integer.parseInt(matcher.group(6));
                    }
                }

                if (showOverlay) {
                    String msg = matcher.group(0).substring(matcher.group(0).indexOf(":") + 2);
                    OverlayManager.getInstance().showNotificationOverlay(in, matcher.group(1), matcher.group(2), matcher.group(3), stashTab, x, y, msg);
                }
            }
        }

        // Process line for TTS
        if (useTTS && voice != null && ttsParseConfig != null) {
            if (ttsParseConfig.getPatternToOutput().isEmpty()) {
                try {
                    textToSpeech(newLine);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            if (useIncludeExclude) {
                if (wordIncludeTextField != null && !wordIncludeTextField.getText().isEmpty()) {
                    String[] includeWords = wordIncludeTextField.getText().split(",");
                    String[] excludeWords = wordExcludeTextField.getText().split(",");
                    String includedWord = "Nothing";
                    boolean included = false;
                    boolean excluded = false;

                    for (String include : includeWords) {
                        if (!include.isEmpty() && Main.containsIgnoreCase(newLine, include)) {
                            included = true;
                            includedWord = include;
                            break;
                        }
                    }

                    if (included) {
                        for (String exclude : excludeWords) {
                            if (!exclude.isEmpty() && newLine.contains(exclude)) {
                                excluded = true;
                                break;
                            }
                        }

                        if (!excluded) {
                            try {
                                String notify;
                                if (ttsParseConfig == null || (notify = ttsParseConfig.getPlaceholders().get("shoutout")) == null) {
                                    notify = getRandomString(shoutOuts);
                                } else {
                                    notify = replacePlaceholders(getRandomString(notify.split(";")).split("\\s+"));
                                }
                                //TODO: Playername in shoutout
//                                notify = notify.replace("(1)", playerName);
                                notify = notify.replace("(1)", includedWord);

                                textToSpeech(notify);
                                return;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            for (PatternOutput patternOutput : ttsParseConfig.getPatternToOutput()) {
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
                        String groupInput = m.group(g);
                        groupInput = groupInput.replace("_", " ");
                        // Check if readable
                        if (!allowedChars.matcher(groupInput).matches()) {
                            groupInput = "something i can not pronounce";
                        }
                        voiceOutput = voiceOutput.replace("(" + g + ")", groupInput);

                        log.append(g)
                                .append(":(")
                                .append(m.group(g))
                                .append(") ");
                    }
                    LogManager.getInstance().log(getClass(), log.toString());

                    String[] words = voiceOutput.split("\\s+");

                    try {
                        textToSpeech(replacePlaceholders(words));
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                    String replacementPart = ttsParseConfig.getPlaceholders().get(replacementKey);
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

    private Boolean balconAvailable;

    public boolean isBalconAvailable() {
        if (balconAvailable == null) {
            balconAvailable = new File("balcon.exe").exists();
        }
        return balconAvailable;
    }

    public boolean isVoiceSupported(String preferredVoice) {
        return getSupportedVoices().contains(preferredVoice);
    }

    private List<String> supportedVoices = new LinkedList<>();

    public List<String> getSupportedVoices() {
        if (isBalconAvailable() && supportedVoices.isEmpty()) {
            try {
                String[] voices = executeCommand("balcon -l").split("\n");

                for (int i = 2; i < voices.length; i++) {
                    supportedVoices.add(voices[i].trim());

                    if (PropertyManager.getInstance().getProp(PropertyManager.VOICE_SPEAKER, null) == null
                            && voices[i].trim().equals(bestVoiceEver)) {
                        setVoice(bestVoiceEver);
                    }
                }
            } catch (InterruptedException | IOException e) {
                LogManager.getInstance().log(getClass(), "Exception during checking voice support. " + e);
                return null;
            }
        }
        return supportedVoices;
    }

    public void startLogParsing() {
        String dir = PropertyManager.getInstance().getPathOfExilePath();
        if (dir.charAt(dir.length() - 1) != '/') {
            dir += "/";
        }

        File file = new File(dir + "\\logs\\Client.txt");
        if (file.exists()) {
            startLogTail(file);

            isRunning = true;
            PropertyManager.getInstance().setProp("do_log_parsing", "true");

            if (showOverlay) {
                // init, load config etc
                OverlayManager.getInstance();
            }
        } else {
            LogManager.getInstance().log(getClass(), "Check your PoE Path! Log file not found. " + file.getAbsolutePath() + " does not exist.");
            onShutdown();
        }
    }

    public void stopLogParsing() {
        PropertyManager.getInstance().setProp("do_log_parsing", "false");

        if (logTailer != null) {
            logTailer.stopRunning();
            logTailer = null;
        }
    }

    public void shutdown() {
        if (logTailer != null) {
            logTailer.stopRunning();
            logTailer = null;
        }

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private String getRandomString(String... list) {
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
        for (String word : words) {
            if (!first) {
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

        if (interrupt && !speechProcesses.isEmpty()) {
            for (Process speechProcess : speechProcesses) {
                if (speechProcess.isAlive()) {
                    speechProcess.destroy();
                }
            }
            speechProcesses.clear();
        }

        String voiceParam = "";
        if (voice != null) {
            voiceParam = " -n " + voice;
        }
        speechProcesses.add(Runtime.getRuntime().exec("balcon -t \"" + text + "\"" + voiceParam + " -v " + volume + " -s " + speed));

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

    public void processTestMessage(boolean in) {
        String msg = getRandomString(in ? inTestMessages : outTestMessages);
        processNewLine(msg);
        LogManager.getInstance().log(getClass(), "Test: " + msg.substring(msg.indexOf("@")));
    }

    public void testSpeech() {
        String test;
        if (ttsParseConfig == null || (test = ttsParseConfig.getPlaceholders().get("test")) == null) {
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

    public ParseConfig getTtsParseConfig() {
        return ttsParseConfig;
    }

    public void notifyBadTendency() throws IOException {
        String badTendencyString = ttsParseConfig.getPlaceholders().get("bad_tendency");
        if (badTendencyString == null) {
            badTendencyString = getRandomString("Update your offers");
        }

        textToSpeech(badTendencyString);
    }

    public void setUseTTS(boolean enabled) {
        if (enabled) {
            loadTTSConfig();
        }
        useTTS = enabled;
    }

    public boolean doUseTTS() {
        return useTTS;
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

    public void openTTSConfig() {
        if (voice == null) {
            LogManager.getInstance().log(getClass(), "TTS is not supported.");
            return;
        }
        File file = configFile;
        if (file.exists()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "TTS Config not found!");
            }
        } else {
            loadTTSConfig();
        }
    }

    public boolean loadTTSConfig() {
        if (voice != null) {
            File file = configFile;
            if (file.exists()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    ParseConfig config = mapper.readValue(file, ParseConfig.class);
                    LogManager.getInstance().log(getClass(), "TTS config '" + configFile.getName() + "' successfully loaded.");
                    setTTSParseConfig(config);
                    return true;
                } catch (JsonMappingException j) {
                    LogManager.getInstance().log(getClass(), "TTS config '" + configFile.getName() + "' corrupted! " + j.getMessage());
                } catch (IOException e) {
                    LogManager.getInstance().log(getClass(), "Error while loading TTS config '" + configFile.getName() + "'");
                    e.printStackTrace();
                }
            } else {
                try {
                    URL url = Main.class.getResource("default_ttsconfig.json");
                    ObjectMapper mapper = new ObjectMapper();
                    ParseConfig config = mapper.readValue(url, ParseConfig.class);
                    LogManager.getInstance().log(getClass(), "Using default TTS config.");

                    storeConfig(config);
                    setTTSParseConfig(config);
                    return true;
                } catch (JsonMappingException j) {
                    LogManager.getInstance().log(getClass(), "TTS Config corrupted! " + j.getMessage());
                } catch (IOException e) {
                    LogManager.getInstance().log(getClass(), "Error while loading TTS config.");
                    e.printStackTrace();
                }
            }
            LogManager.getInstance().log(getClass(), "Loading TTS config failed.");
            return false;
        }
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

        if (speechProcesses != null && !speechProcesses.isEmpty()) {
            for (Process speechProcess : speechProcesses) {
                speechProcess.destroy();
            }
            speechProcesses.clear();
        }
    }

    public interface Listener {
        void onStarted();

        void onShutDown();
    }
}
