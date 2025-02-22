package com.flashcards.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Document("deck")
public class Deck {
    @Id
    private String id;
    private String name;
    private String owner;
    private ArrayList<String> cards;

    public Deck(String id, String name, String owner, ArrayList<String> cards) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.cards = cards;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ArrayList<String> getCards() {
        return cards;
    }

    public void setCards(ArrayList<String> cards) {
        this.cards = cards;
    }

    @Override
    public String toString() {
        return String.format(
                "Deck[id=%s, name='%s', owner=%s, cards=%s]",
                id, name, owner, cards.toString()
        );
    }
}