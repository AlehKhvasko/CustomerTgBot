package com.telegarambot.customertgbot.service;

import com.telegarambot.customertgbot.config.BotConfig;
import com.telegarambot.customertgbot.model.User;
import com.telegarambot.customertgbot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final UserRepository userRepository;
    final BotConfig botConfig;

    final static private String HELP_INFO = "Bot was created to demostrate Spring capabilities. \n\n"+
            "You can execute commands from the main menu on left or by typing it.\n\n "+
            "Type /start to see welcome message.\n\n"+
            "Type /help to see this message again.";


    public TelegramBot(BotConfig botConfig, UserRepository userRepository) {
        this.botConfig = botConfig;
        List<BotCommand> botCommandList = new ArrayList<>();
        botCommandList.add(new BotCommand("/start", "get a welcome greatings"));
        botCommandList.add(new BotCommand("/mydata", "get your data stored"));
        botCommandList.add(new BotCommand("/deletedata", "delete your data"));
        botCommandList.add(new BotCommand("/help", "info how to use a bot"));
        botCommandList.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(botCommandList, new BotCommandScopeDefault(), null));
        }catch (TelegramApiException e){
            log.error("Error setting bots command list " + e.getMessage());
        }
        this.userRepository = userRepository;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            switch (messageText){
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    //log.info("Bot started");
                    registerUser(update.getMessage());
                    break;
                case "/help":
                    sendMessage(chatId, HELP_INFO);
                    break;

                default:
                    sendMessage(chatId, "Sorry, command was not recognized");
            }
        }
    }

    private void startCommandReceived(Long chatId, String name){
        String answer = EmojiParser.parseToUnicode("Hi " + name + " nice to meet you. " + ":blush:");
        //String answer = "Hi " + name + " nice to meet you. ";
        sendMessage(chatId, answer);
    }

    private void sendMessage(Long chatId, String textToSend){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try{
            execute(sendMessage);
        }
        catch (TelegramApiException e){
            //log.error("Error in sending message " + e.getMessage());
        }


    }

    private void registerUser(Message message) {
        Long chatId = message.getChatId();
        if (userRepository.findById(chatId).isEmpty()){
            var chat = message.getChat();
            User user = new User();

            user.setChatId(chatId);
            user.setUserName(chat.getUserName());
            user.setLastName(chat.getLastName());
            user.setFirstName(chat.getFirstName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            //log.info("user saved" + user)

        }
    }

}
