package com.finsight.web.restful.card;

import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.KeyValue;
import com.finsight.web.restful.model.TreeNode;
import com.finsight.application.card.CardFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
public class CardNumberController {

    @Autowired
    private CardFacade cardFacade;

    @GetMapping("/numbers")
    public List<KeyValue> list(@RequestParam("bankCode") String bankCode,
                               @RequestParam("cardTypeCode") String cardTypeCode){
        return cardFacade.listNumbers(bankCode, cardTypeCode);
    }

    @GetMapping("/name")
    public java.util.Map<String,String> name(@RequestParam("bankCode") String bankCode,
                         @RequestParam("cardTypeCode") String cardTypeCode,
                         @RequestParam("cardNo") String cardNo){
        return cardFacade.cardName(bankCode, cardTypeCode, cardNo);
    }

    @GetMapping("/list")
    public List<KeyValue> allCards(){
        return cardFacade.allCards();
    }

    @GetMapping
    public List<BankCard> listCards(@RequestParam(value = "cardTypeCode", required = false) String cardTypeCode){
        return cardFacade.listCards(cardTypeCode);
    }

    @PostMapping
    public BankCard add(@RequestBody BankCard card){
        return cardFacade.add(card);
    }

    @PutMapping("/{id}")
    public BankCard update(@PathVariable("id") String id, @RequestBody BankCard card){
        return cardFacade.update(id, card);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id){
        cardFacade.delete(id);
    }

    @GetMapping("/tree")
    public List<TreeNode> tree(){
        return cardFacade.tree();
    }
}
