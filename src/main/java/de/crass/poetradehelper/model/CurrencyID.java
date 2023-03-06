package de.crass.poetradehelper.model;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class CurrencyID {
    public static CurrencyID CHAOS = new CurrencyID(1, "chaos", "Chaos Orb");
    public static CurrencyID EXALTED = new CurrencyID(2, "exalted", "Exalted Orb");
    public static CurrencyID DIVINE = new CurrencyID(3, "divine", "Divine Orb");
    public static CurrencyID ALCHEMY = new CurrencyID(4, "alch", "Orb of Alchemy");
    public static CurrencyID REGAL = new CurrencyID(7, "regal", "Regal Orb");

    private int id;
    private final String tradeID;
    private final String displayName;
    private final String iconUrl;
    static Set<CurrencyID> values = new HashSet<>();

    public CurrencyID(JSONObject object) {
        id = object.getInt("id");
        displayName = object.getString("name");
        tradeID = object.getString("tradeId");
        if (!object.isNull("icon")) {
            iconUrl = object.getString("icon");
        } else {
            iconUrl = null;
        }
    }

    private CurrencyID(int ID, String tradeID, String displayName) {
        this(ID, tradeID, displayName, null);
    }

    private CurrencyID(int ID, String tradeID, String displayName, String iconUrl) {
        id = ID;
        this.tradeID = tradeID;
        this.displayName = displayName;
        this.iconUrl = iconUrl;
    }

    public void store(){
        values.add(this);
    }

    public int getID() {
        return id;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public static CurrencyID get(int ID) {
        for (CurrencyID id : getValues()) {
            if (id.getID() == ID) {
                return id;
            }
        }
        return null;
    }

    public static CurrencyID getByTradeID(String ID) {
        if (ID.equals(EXALTED.tradeID)) {
            return EXALTED;
        } else if(ID.equals(CHAOS.tradeID)){
            return CHAOS;
        } else if(ID.equals(DIVINE.tradeID)){
            return DIVINE;
        }

        for (CurrencyID id : getValues()) {
            if (id.getTradeID().equals(ID)) {
                return id;
            }
        }

        return null;
    }

    public static CurrencyID getByDisplayName(String ID) {
        for (CurrencyID id : getValues()) {
            if (id.getDisplayName().equals(ID)) {
                return id;
            }
        }
        return null;
    }

    public static Set<CurrencyID> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static CurrencyID getRandom(){
        int size = values.size();
        int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        int i = 0;
        for(CurrencyID obj : values)
        {
            if (i == item)
                return obj;
            i++;
        }
        return null;
    }

    public String getTradeID() {
        return tradeID;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CurrencyID that = (CurrencyID) o;
        return this.id == that.id || this.tradeID.equals(that.tradeID);
    }

    @Override
    public int hashCode() {
        return id;
    }

    // FIXME Fetch from online sounce - now only works for 20stack items and predefined.
    public int getStackSize() {
        int stackSize = 20;
        if (CurrencyID.CHAOS.equals(this) || CurrencyID.EXALTED.equals(this) || CurrencyID.ALCHEMY.equals(this) || CurrencyID.REGAL.equals(this)) {
            stackSize = 10;
        }

        return stackSize;
    }
}