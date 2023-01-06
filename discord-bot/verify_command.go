package main

import (
	"errors"
	"fmt"
	"github.com/Goscord/goscord/discord"
	"github.com/Goscord/goscord/discord/embed"
	"gorm.io/gorm"
	"log"
)

type VerifyCommand struct{}

func (c *VerifyCommand) Name() string {
	return "mcverify"
}

func (c *VerifyCommand) Description() string {
	return "Verify a Minecraft account as yours."
}

func (c *VerifyCommand) Category() string {
	return "general"
}

func (c *VerifyCommand) Options() []*discord.ApplicationCommandOption {
	return []*discord.ApplicationCommandOption{
		{
			Type:        discord.ApplicationCommandOptionString,
			Name:        "minecraftname",
			Description: "Minecraft Account Name i.e: Herobrine",
			Required:    true,
		},
		{
			Type:        discord.ApplicationCommandOptionInteger,
			Name:        "verificationcode",
			Description: "Verification code as from the Minecraft server",
			Required:    true,
		},
	}
}

func (c *VerifyCommand) Execute(ctx *Context) bool {
	if CheckGuild(ctx) != nil {
		return false
	}

	accountName := ctx.interaction.Data.Options[0].String()
	verificationCode := ctx.interaction.Data.Options[1].Int()

	// Add to database
	guildid := ctx.interaction.GuildId

	err := db.Transaction(func(tx *gorm.DB) error {
		// Get guild settings
		var gs GuildSettings
		mdl := tx.Model(&gs)
		err := mdl.Error
		if err != nil {
			return err
		}

		err = mdl.First(&gs, guildid).Error
		if err != nil {
			return err
		}

		// Check for access role
		if !Contains(ctx.interaction.Member.Roles, gs.AccessRole) {
			return errors.New(fmt.Sprintf("Invalid Permissions - you require the role <@&%s> to use this command", gs.AccessRole))
		}

		// Check that the user is not banned
		discordUser := DiscordUser{
			HasAdminRole:  UserIsAdmin(gs, ctx.interaction.Member),
			Banned:        false,
			DiscordUserID: ctx.interaction.Member.User.Id,
		}
		mdl = tx.Model(&discordUser)
		err = mdl.Error
		if err != nil {
			return err
		}

		// Get the discord minecraft user
		dcmcUser := DiscordMinecraftUser{}
		dcmcmdl := tx.Model(&dcmcUser)
		err = dcmcmdl.Error
		if err != nil {
			return err
		}

		err = dcmcmdl.First(&dcmcUser, "discord_user_id = ? AND minecraft_user = ?", ctx.interaction.Member.User.Id, accountName).Error
		if err != nil {
			return err
		}

		if dcmcUser.Verified == true {
			return errors.New("This user is already verified")
		}

		// Get the minecraft user with the accountName
		mcUser := MinecraftUser{}
		mdl = tx.Model(&mcUser)
		err = mdl.Error
		if err != nil {
			return err
		}

		err = mdl.Select("username", "verification_number", "banned").First(&mcUser, "username = ?", accountName).Error
		if err != nil {
			return err
		}

		log.Print(mcUser.VerificationNumber)
		log.Print(verificationCode)
		if verificationCode != mcUser.VerificationNumber {
			return errors.New("Incorrect verification code")
		}

		// Mark the user as verified
		err = dcmcmdl.Updates(map[string]interface{}{
			"verified": true,
		}).Error
		if err != nil {
			return err
		}

		return nil
	})
	if err != nil {
		SendInternalError(err, ctx)
		return false
	}

	e := embed.NewEmbedBuilder()
	message := fmt.Sprintf(`**User:** <@%s>
**Minecraft User:** %s`,
		ctx.interaction.Member.User.Id,
		accountName)

	e.SetTitle("Verified a Minecraft User")
	e.SetDescription(message)
	ThemeEmbed(e, ctx)

	// Send response
	ctx.client.Interaction.CreateResponse(ctx.interaction.Id,
		ctx.interaction.Token,
		&discord.InteractionCallbackMessage{Embeds: []*embed.Embed{e.Embed()},
			Flags: discord.MessageFlagUrgent})

	log.Printf("<@%s> verifed Minecraft account %s", ctx.interaction.Member.User.Id, accountName)

	return true
}
