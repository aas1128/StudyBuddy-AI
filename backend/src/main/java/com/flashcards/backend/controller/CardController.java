package com.flashcards.backend.controller;

import com.flashcards.backend.model.Card;
import com.flashcards.backend.persistence.CardDAO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("card")
public class CardController {
    private static final Logger LOG = Logger.getLogger(CardController.class.getName());
    private CardDAO cardDAO;
    private ChatClient chatClient;

    public CardController(CardDAO cardDAO, ChatClient.Builder builder) {
        this.cardDAO = cardDAO;
        this.chatClient = builder.build();
    }

//    @GetMapping("/get/{id}")
//    public ResponseEntity<ArrayList<Card>> getCardById(@PathVariable String id) {
//        LOG.log(Level.INFO, "GET /get/{0}", id);
//
//        try {
//            ArrayList<String> ids = new ArrayList<>(Arrays.asList(id.split(",")));
//            ArrayList<Card> cards = new ArrayList<>(cardDAO.findAllById(ids));
//            if (!cards.isEmpty()) {
//                return new ResponseEntity<>(cards, HttpStatus.OK);
//            } else {
//                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//            }
//        } catch (Exception e) {
//            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @GetMapping("/deck/{deckId}")
    public ResponseEntity<ArrayList<Card>> getCardsByDeckId(@PathVariable String deckId) {
        LOG.log(Level.INFO, "GET /deck/{0}", deckId);

        try {
            ArrayList<Card> cards = new ArrayList<>(cardDAO.findCardsByDeckId(deckId));
            if (!cards.isEmpty()) {
                return new ResponseEntity<>(cards, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Card> createCard(@Valid @RequestBody Card card) {
        LOG.log(Level.INFO, "POST /create {0}", card);

        try {
            card.setId(null);
            Card new_deck = cardDAO.save(card);
            return new ResponseEntity<>(new_deck, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/create-many")
    public ResponseEntity<ArrayList<Card>> createCards(@Valid @RequestBody ArrayList<Card> cards) {
        LOG.log(Level.INFO, "POST /create-many {0}", cards);

        try {
            for (Card card: cards) {
                card.setId(null);
            }

            ArrayList<Card> allCards = new ArrayList<>(cardDAO.saveAll(cards));
            return new ResponseEntity<>(allCards, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Card> deleteCard(@PathVariable String id) {
        LOG.log(Level.INFO, "POST /delete/{0}", id);

        try {
            if (cardDAO.findById(id).isPresent()) {
                cardDAO.deleteById(id);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<Card> updateCard(@Valid @RequestBody Card card) {
        LOG.log(Level.INFO, "POST /update/{0}", card);

        try {
            if (cardDAO.findById(card.getId()).isPresent()) {
                Card updated_card = cardDAO.save(card);
                return new ResponseEntity<>(updated_card, HttpStatus.CREATED);

            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path="/generate", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> generate(@RequestPart("file") MultipartFile file, @RequestPart("prompt") String userPrompt) {
        LOG.log(Level.INFO, "POST /generate", file);
        ArrayList<Media> medias = new ArrayList<>();
        String generationPrompt;
        try {
        if (file.isEmpty()) {
            generationPrompt = String.format(
                    """
                    Given the topic: %s generate flash cards in this format:
                    [
                      {
                        "front": "front text",
                        "back": "back"
                      },
                      {
                        "front": "front text",
                        "back": "back"
                      },
                      {
                        "front": "front text",
                        "back": "back"
                      },
                      ...
                    ]
                    
                    Make sure to only include the JSON and not anything else.
                
                    """, userPrompt);
        } else {
             generationPrompt = String.format(
                    """
                    Given my lecture notes attached, generate some flash cards in this format:
                    [
                      {
                        "front": "front text",
                        "back": "back"
                      },
                      {
                        "front": "front text",
                        "back": "back"
                      },
                      {
                        "front": "front text",
                        "back": "back"
                      },
                      ...
                    ]
                    
                    Make sure to only include the JSON and not anything else.
                
                    If information is repeated, give general topic info instead.
                    
                    Please pay special attention to these instructions: %s
                    """, userPrompt);

                PDDocument document = PDDocument.load(file.getBytes())
                PDFRenderer renderer = new PDFRenderer(document);
                PDPageTree pages = document.getPages();


                for (PDPage page : pages) {
                    int pageIndex = document.getPages().indexOf(page);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(renderer.renderImageWithDPI(pageIndex, 300), "PNG", baos);

                    medias.add(new Media(
                            MimeType.valueOf("image/png"),
                            new ByteArrayResource(baos.toByteArray())
                    ));
                }
        }

            UserMessage userMessage = new UserMessage(file_prompt, medias);
            Prompt prompt = new Prompt(userMessage);
            String response = chatClient.prompt(prompt).call().content();
            if (response == null || response.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                response = response.substring(response.indexOf('['), response.indexOf(']') + 1);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}




