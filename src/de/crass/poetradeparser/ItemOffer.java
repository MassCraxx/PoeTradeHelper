package de.crass.poetradeparser;

import org.json.JSONObject;

public class ItemOffer {

    private String itemName;
    private String itemType;
    private String characterName;
    private int price;
    private String currency;

    public ItemOffer(String json){
        this(new JSONObject(json));
    }

    public ItemOffer(JSONObject offer) {
        JSONObject result = offer.getJSONArray("result").getJSONObject(0);
        JSONObject listing = result.getJSONObject("listing");
        JSONObject item = result.getJSONObject("item");

        characterName = listing.getJSONObject("account").getString("lastCharacterName");
        JSONObject priceObject = listing.getJSONObject("price");
        price = priceObject.getInt("amount");
        currency = priceObject.getString("currency");

        itemName = item.getString("name");
        itemType = item.getString("typeLine");
        if(itemName.isEmpty()){
            itemName = itemType;
        }
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public String getCharacterName() {
        return characterName;
    }

    public int getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}
