package de.crass.poetradeparser;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

import static de.crass.poetradeparser.PoeTradeWebParser.CurrencyID.*;

public class Main {

    public static final String version = "0.1";

    private JPanel mainPanel;
    private JButton updateButton;
    private JTable currencyTable;
    private JTabbedPane tabbedPane1;
    private JComboBox<PoeTradeWebParser.CurrencyID> currencyComboBox;
    private JList<PoeTradeWebParser.CurrencyID> currencyFilterList;
    private DefaultListModel<PoeTradeWebParser.CurrencyID> filterListModel;
    private JComboBox<PoeTradeWebParser.CurrencyID> currencyFilterCB;
    private JButton currencyFilterAdd;
    private JButton currencyFilterRem;
    private final CurrencyTableModel currencyTableModel;

    private final List<de.crass.poetradeparser.PoeTradeWebParser.CurrencyID> defaultCurrencyFilter = Arrays.asList(
            ALCHEMY,
            SCOURING,
            ALTERATION,
            REGAL,
            CHROMATIC,
            CHANCE);

    public static String currentLeague = "Incursion";
    private PoeTradeWebParser.CurrencyID primaryCurrency = EXALTED;

    public static void main(String[] args) {
        Main app = new Main();
        JFrame jFrame = new JFrame("PoeTradeParser");
        jFrame.setContentPane(app.mainPanel);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    private Main() {
        currencyTableModel = new CurrencyTableModel();
        currencyTable.setModel(currencyTableModel);
        currencyTable.setAutoCreateRowSorter(true);
//        currencyTable.getColumnModel().getColumn(0).setPreferredWidth(200);
//        TableRowSorter<SumTableModel> sorter = new TableRowSorter<>(sumTableModel);
        updateButton.addActionListener(e -> update());

        DefaultComboBoxModel<PoeTradeWebParser.CurrencyID> primaryCurrencyModel = new DefaultComboBoxModel<>(PoeTradeWebParser.CurrencyID.values());
        currencyComboBox.setModel(primaryCurrencyModel);
        currencyComboBox.setSelectedItem(primaryCurrency);
        currencyComboBox.addActionListener(e -> {
            primaryCurrency = (PoeTradeWebParser.CurrencyID) currencyComboBox.getSelectedItem();
        });

        DefaultComboBoxModel<PoeTradeWebParser.CurrencyID> filterCurrencyModel = new DefaultComboBoxModel<>(PoeTradeWebParser.CurrencyID.values());
        currencyFilterCB.setModel(filterCurrencyModel);

        filterListModel = new DefaultListModel<>();
        for (PoeTradeWebParser.CurrencyID id : defaultCurrencyFilter) {
            filterListModel.addElement(id);
        }
        currencyFilterList.setModel(filterListModel);

        currencyFilterAdd.addActionListener(e -> {
            PoeTradeWebParser.CurrencyID selectedItem = (PoeTradeWebParser.CurrencyID) currencyFilterCB.getSelectedItem();
            if (!filterListModel.contains(selectedItem)) {
                filterListModel.addElement(selectedItem);
            }
        });

        currencyFilterRem.addActionListener(e -> {
            PoeTradeWebParser.CurrencyID selectedItem = currencyFilterList.getSelectedValue();
            if (filterListModel.contains(selectedItem)) {
                filterListModel.removeElement(selectedItem);
            }
        });
    }

    private void update() {
        PoeTradeWebParser webParser = new PoeTradeWebParser();
        for (Object secondary : filterListModel.toArray()) {
            if (secondary != primaryCurrency) {
                webParser.fetchCurrencyOffers(primaryCurrency, (PoeTradeWebParser.CurrencyID) secondary, currentLeague);
            }
        }

        currencyTableModel.update(primaryCurrency, webParser.getCurrentOffers(), webParser.getPlayerOffers());
    }

    public static void log(Class clazz, String log) {
        System.out.println("[" + clazz.getSimpleName() + "]: " + log);
    }
}
