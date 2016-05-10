package SocialClient.UI;

import SimpleSocial.Exception.UnregisteredConfigNameException;
import SimpleSocial.Exception.UserExistsException;
import SocialClient.SocialClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * Created by alessandro on 08/05/16.
 */

public class MainForm extends SocialClient {
    JButton logInOut = new JButton("Login");
    JButton updateSession = new JButton("Rinnova token OAuth");
    JButton listaAmici = new JButton("Vedi lista amici");
    JFrame window;

    public MainForm(){
        window = new JFrame("SocialClient");
        Container c = new Container();
        window.setSize(600, 600);
        window.setMinimumSize(new Dimension(600, 600));
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        if(!config.isSet("REGISTERED")){
            JLabel message = new JLabel();
            c.setLayout(new GridLayout(0, 1));
            JLabel title = new JLabel("REGISTRAZIONE", SwingConstants.CENTER);
            c.add(title);
            c.add(message);
            c.add(new JLabel("Username: "));
            JTextField user = new JTextField();
            c.add(user);
            c.add(new JLabel("Password: "));
            JPasswordField psw = new JPasswordField();
            psw.setEchoChar('*');
            c.add(psw);
            JButton submit = new JButton("Submit");
            JButton next = new JButton("Procedi");
            next.setEnabled(false);
            submit.addActionListener(e -> {
                try{
                    if(register(user.getText(), new String(psw.getPassword()))) {
                        message.setText("L'utente è stato registrato con successo.");
                        next.setEnabled(true);
                    }else{
                        message.setText("Errore durante la registrazione. Controllare connessione.");
                    }
                }catch (UserExistsException e1) {
                    message.setText("Errore nella registrazione. "+e1.getMessage());
                }
            });
            next.addActionListener(e -> {
                window.getContentPane().remove(0);
                window.getContentPane().add(this.MainConteiner(), BorderLayout.CENTER);
                window.getContentPane().invalidate();
                window.revalidate();

            });
            c.add(submit);
            c.add(next);
            window.add(c, BorderLayout.CENTER);
            window.setVisible(true);
        }
    }

    private Container MainConteiner(){
        if(!this.isValidSession()){
            return this.LoginConteiner();
        }
        else{
            return this.MenuConteiner();
        }
    }

    private Container LoginConteiner(){
        JLabel message = new JLabel();
        Container c = new Container();
        c.setLayout(new GridLayout(0, 1));
        JLabel title = new JLabel("LOGIN", SwingConstants.CENTER);
        c.add(title);
        c.add(message);
        c.add(new JLabel("Username:"));
        JTextField user = new JTextField();
        c.add(user);
        c.add(new JLabel("Password:"));
        JPasswordField psw = new JPasswordField();
        psw.setEchoChar('*');
        ActionListener performLogin = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                {
                    if(login(user.getText(), new String(psw.getPassword()))){
                        window.setContentPane(MainConteiner());
                        window.revalidate();
                    }
                    else
                        message.setText("User o password errata. Riprovare.");
                }
            }
        };
        psw.addActionListener(performLogin);
        c.add(psw);
        JButton submit = new JButton("Submit");
        submit.addActionListener(performLogin);

        c.add(submit);

        return c;
    }

    private Container MenuConteiner(){
        Container c = new Container();
        GridBagConstraints constraints = new GridBagConstraints();
        c.setLayout(new GridBagLayout());

        JLabel title;
        try {
            title = new JLabel("<html><b>Benvenuto "+config.getValue("USER")+"</b></html>");
        } catch (UnregisteredConfigNameException e) {
            title = new JLabel("<html><b>Benvenuto</b></html>");
        }
        constraints.gridheight = 1;
        constraints.gridwidth = 4;
        constraints.gridx = 0;
        constraints.gridy = 0;
        c.add(title, constraints);

        CustomTextField searchfield = new CustomTextField(30);
        searchfield.setPlaceholder("Ricerca utenti");
        JLabel usersFound = new JLabel();
        JScrollPane scrollableList = new JScrollPane(usersFound);
        searchfield.addActionListener(e -> {
            Vector<String> result = this.searchUser(searchfield.getText());
            usersFound.setText("<html>");
            for(String u : result)
                usersFound.setText(usersFound.getText()+u+"<br/>");
            usersFound.setText(usersFound.getText()+"</html>");
            window.revalidate();
        });
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        c.add(searchfield, constraints);
        constraints.gridy = 2;
        constraints.gridheight = 4;
        constraints.gridwidth = 2;
        c.add(scrollableList, constraints);

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            logout();
            window.setContentPane(MainConteiner());
            window.revalidate();
        });
        constraints.gridx = 3;
        constraints.gridy = 0;
        c.add(logout, constraints);
        return c;
    }

    public static void main(String[] args){
        new MainForm();
    }
}
