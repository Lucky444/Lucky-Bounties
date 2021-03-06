package de.lucky44.luckybounties.system;

import de.lucky44.luckybounties.LuckyBounties;
import de.lucky44.luckybounties.util.bounty;
import de.lucky44.luckybounties.util.permissionType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public class eventManager implements Listener {

    @EventHandler
    public static void OnSlotClick(InventoryClickEvent e){

        Inventory I = e.getClickedInventory();
        Player p = (Player) e.getWhoClicked();
        int clickedSlot = e.getSlot();

        if(I == null){
            return;
        }

        ItemStack clickedItem = I.getItem(clickedSlot);

        if(clickedItem == null || (I.getType() != InventoryType.CHEST && I.getType() != InventoryType.DISPENSER)){
            return;
        }

        String invName = ChatColor.stripColor(e.getView().getTitle()).toLowerCase();

        if(!(invName.equals("bounties") || invName.contains("'"))){
            return;
        }
        else if(invName.contains("'")){
            if(!invName.split("'")[1].equals("s bounties") && !invName.split("'")[1].equals("s head")){
                return;
            }
        }

        if(invName.equals("bounties")){

            //Check if head was clicked
            if(clickedItem.getType().equals(Material.PLAYER_HEAD)){
                SkullMeta sKM = (SkullMeta) clickedItem.getItemMeta();

                assert sKM != null;
                String UUID = sKM.getOwningPlayer().getUniqueId().toString();

                guiManager.showSpecificMenu(p, Bukkit.getPlayer(java.util.UUID.fromString(UUID)));
            }

            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
        }
        else if(invName.split("'")[1].equals("s bounties")){

            //Check if set bounty was clicked
            if(clickedItem.getType() == Material.AMETHYST_SHARD && clickedSlot == 13){
                SkullMeta sKM = (SkullMeta) I.getItem(4).getItemMeta();

                assert sKM != null;
                String UUID = sKM.getOwningPlayer().getUniqueId().toString();

                if(LuckyBounties.instance.useItems && !LuckyBounties.instance.economy)
                    guiManager.showBountySetMenu(p,Bukkit.getPlayer(java.util.UUID.fromString(UUID)));

                if(LuckyBounties.instance.economy) {

                    bounty mB = LuckyBounties.getEcoBounty(UUID);

                    if(mB != null) {
                        mB.moneyPayment += LuckyBounties.instance.eco_amount;
                    }
                    else {
                        bounty b = new bounty(UUID,LuckyBounties.instance.eco_amount);
                        LuckyBounties.bounties.add(b);
                    }

                    Player p1 = Bukkit.getPlayer(java.util.UUID.fromString(UUID));
                    String p1Name = "NAN";
                    if(p1 != null)
                        p1Name = p1.getDisplayName();

                    if(LuckyBounties.instance.useMessages){
                        String mS = LuckyBounties.instance.setPlayerMessage.replace("{player}",p.getDisplayName()).replace("{amount}", Float.toString(LuckyBounties.instance.eco_amount) + LuckyBounties.instance.economy_name).replace("{target}", p1Name);
                        Bukkit.broadcastMessage(mS);
                    }

                    LuckyBounties.doShit(p, LuckyBounties.instance.eco_amount, 0);
                    guiManager.showSpecificMenu(p, p1);
                }
            }
            else if(clickedItem.getType() == Material.FEATHER && clickedSlot == 8 && p.isOp()){

                //Clear bounties
                SkullMeta sKM = (SkullMeta) I.getItem(4).getItemMeta();

                assert sKM != null;
                String UUID = sKM.getOwningPlayer().getUniqueId().toString();

                LuckyBounties.clearBounties(UUID);

                e.setResult(Event.Result.DENY);

                guiManager.showSpecificMenu(p,Bukkit.getPlayer(java.util.UUID.fromString(UUID)));
                return;
            }

            boolean allow = false;

            //TODO: Make individual bounties removable again
/*            if(clickedSlot > 17 && clickedSlot < 54  && isAllowedToClick(p)){
                allow = true;

                bounty r = null;

                for(bounty b : LuckyBounties.bounties){
                    if(b.payment.converted == clickedItem){
                        r = b;
                    }
                }

                if(r != null){
                    LuckyBounties.bounties.remove(r);
                }
            }*/

            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
        }
        else if(invName.split("'")[1].equals("s head")){

            if(clickedSlot != 4 && clickedSlot <= 8){

                SkullMeta sKM = (SkullMeta) I.getItem(1).getItemMeta();

                assert sKM != null;
                String UUID = sKM.getOwningPlayer().getUniqueId().toString();

                //Cancel bounty set
                if(clickedSlot == 6){
                    guiManager.cancelBounty(p, Bukkit.getPlayer(java.util.UUID.fromString(UUID)));
                }
                else if(clickedSlot == 8){ //Confirm bounty set
                    guiManager.confirmBounty(p, Bukkit.getPlayer(java.util.UUID.fromString(UUID)));
                }

                e.setResult(Event.Result.DENY);
                e.setCancelled(true);
            }
        }
    }

    static boolean isAllowedToClick(Player sender){
        return (sender.isOp() && LuckyBounties.instance.remove == permissionType.OP) || (sender.hasPermission("lb.op") && LuckyBounties.instance.remove == permissionType.LB) || ((sender.hasPermission("lb.op") || sender.isOp()) && LuckyBounties.instance.remove == permissionType.BOTH);
    }

    @EventHandler
    public static void onKill(PlayerDeathEvent e){
        Player killed = e.getEntity();
        Player killer = e.getEntity().getKiller();

        //Check if Player was killed
        if(killer != null){

            List<bounty> bounties = LuckyBounties.getBounties(killed.getUniqueId().toString());

            if(bounties.size() == 1 && !LuckyBounties.instance.messageSing.equals("")){
                String m = LuckyBounties.instance.messageSing.replace("{killer}", killer.getDisplayName()).replace("{killed}",killed.getDisplayName());
                e.setDeathMessage(m);
            }
            else if(bounties.size() > 1 && !LuckyBounties.instance.messageMulti.equals("")){
                String m = LuckyBounties.instance.messageMulti.replace("{killer}", killer.getDisplayName()).replace("{killed}",killed.getDisplayName());
                e.setDeathMessage(m);
            }
            else if(bounties.size() == 0 && !LuckyBounties.instance.killBounty.equals("false")){
                String command = LuckyBounties.instance.killBounty;

                String playerName = "";

                playerName = killer.getName();

                command = command.replace("{killer}",playerName);

                Bukkit.dispatchCommand(LuckyBounties.instance.console, command);
            }

            //Drop the bounties of killed player
            for(bounty b : bounties){

                if(b.type == 0) {

                    World dropWorld = killed.getWorld();

                    dropWorld.dropItem(killed.getLocation(),b.payment.converted);
                }
                else{

                    LuckyBounties.doShit(killer, b.moneyPayment, 1);

                }
            }

            //Clear the bounties of killed player
            LuckyBounties.clearBounties(killed.getUniqueId().toString());
        }
    }
}
