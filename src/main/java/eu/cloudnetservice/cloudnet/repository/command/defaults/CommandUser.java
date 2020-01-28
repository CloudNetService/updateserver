package eu.cloudnetservice.cloudnet.repository.command.defaults;

import de.dytanic.cloudnet.common.Properties;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.command.Command;
import eu.cloudnetservice.cloudnet.repository.command.ICommandSender;
import eu.cloudnetservice.cloudnet.repository.web.WebPermissionRole;

import java.util.Arrays;
import java.util.Collection;

public class CommandUser extends Command {

    private CloudNetUpdateServer updateServer;

    public CommandUser(CloudNetUpdateServer updateServer) {
        super("user");
        this.updateServer = updateServer;
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {

            Collection<String> users = this.updateServer.getDatabase().getUserNames();
            for (String user : users) {
                sender.sendMessage(" - " + user + "@" + this.updateServer.getDatabase().getRole(user));
            }
            sender.sendMessage("=> Total registered users: " + users.size());

        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {

            String user = args[1];

            if (this.updateServer.getDatabase().containsUser(user)) {
                sender.sendMessage("That user already exists!");
                return;
            }

            this.updateServer.getDatabase().insertUser(user);

            sender.sendMessage("The user has been created successfully");

        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {

            String user = args[1];

            if (!this.updateServer.getDatabase().containsUser(user)) {
                sender.sendMessage("That user doesn't exist!");
                return;
            }

            this.updateServer.getDatabase().deleteUser(user);

            sender.sendMessage("The user has been deleted successfully");

        } else if (args.length == 3 && args[0].equalsIgnoreCase("role")) {

            String user = args[1];
            WebPermissionRole role;
            try {
                role = WebPermissionRole.valueOf(args[2]);
            } catch (Throwable throwable) {
                sender.sendMessage("That role doesn't exist! Available roles:" + Arrays.toString(WebPermissionRole.values()));
                return;
            }

            if (!this.updateServer.getDatabase().containsUser(user)) {
                sender.sendMessage("That user doesn't exist!");
                return;
            }

            this.updateServer.getDatabase().updateUserRole(user, role);

            sender.sendMessage("The role of the user " + user + " has been successfully updated");

        } else if (args.length == 2 && args[0].equalsIgnoreCase("role")) {

            String user = args[1];

            if (!this.updateServer.getDatabase().containsUser(user)) {
                sender.sendMessage("That user doesn't exist!");
                return;
            }

            sender.sendMessage("Role of the user " + user + ": " + this.updateServer.getDatabase().getRole(user));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("password") && args[1].equalsIgnoreCase("check")) {

            String user = args[2];
            String password = args[3];

            if (!this.updateServer.getDatabase().containsUser(user)) {
                sender.sendMessage("That user doesn't exist!");
                return;
            }

            boolean correctPassword = this.updateServer.getDatabase().checkUserPassword(user, password);
            sender.sendMessage("The password is " + (correctPassword ? "correct" : "invalid"));

        } else if (args.length == 4 && args[0].equalsIgnoreCase("password") && args[1].equalsIgnoreCase("change")) {

            String user = args[2];
            String password = args[3];

            if (!this.updateServer.getDatabase().containsUser(user)) {
                sender.sendMessage("That user doesn't exist!");
                return;
            }

            this.updateServer.getDatabase().updateUserPassword(user, password);
            sender.sendMessage("Successfully updated the password for the user " + user);

        } else {
            sender.sendMessage(
                    "user list | list all users",
                    "user create <name> | create a new user",
                    "user delete <name> | delete an existing user",
                    "user password check <name> <password> | check the password of a player",
                    "user password change <name> <newPassword> | change the password of a player",
                    "user role <name> | show the role of a user",
                    "user role <name> <newRole> | change the role of a user"
            );
        }
    }
}
