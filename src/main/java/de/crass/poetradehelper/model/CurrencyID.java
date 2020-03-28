package de.crass.poetradehelper.model;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class CurrencyID {
    private int id;
    private String tradeID;
    private String displayName;

    static Set<CurrencyID> values = new HashSet<>();

    public CurrencyID(JSONObject object){
        id = object.getInt("poeTradeId");
        displayName = object.getString("name");
        tradeID = object.getString("tradeId");
    }

    public CurrencyID(int ID, String tradeID, String displayName) {
        id = ID;
        this.tradeID = tradeID;
        this.displayName = displayName;
    }

    public void store(){
        values.add(this);
    }

    public int getID() {
        return id;
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
        for (CurrencyID id : getValues()) {
            if (id.getTradeID().equals(ID)) {
                return id;
            }
        }

        if (ID.equals("exa")) {
            return new CurrencyID(6, "exa", "Exalted Orb");
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
}