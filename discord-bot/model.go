package main

import (
	"errors"
	"fmt"
	"github.com/Goscord/goscord/discord"
	"github.com/Goscord/goscord/discord/embed"
	"github.com/Goscord/goscord/gateway"
	"github.com/jackc/pgtype"
	"gorm.io/gorm"
	"log"
	"time"
)

// Bot database model - a "singleton" for the server
type GuildSettings struct {
	gorm.Model
	// Guild Id
	ID string `gorm:"primaryKey"`
	// This is the admin role (@Committe 2022/23)
	AdminRole string
	// Role to get access to the system (@Student)
	AccessRole string
	// A master toggle to turn off user registrations
	AllowUserRegistrations bool
	MaxAccountsPerUser     int64
}

type DiscordUser struct {
	gorm.Model
	// This is a cache of whether or not the user has the admin role
	HasAdminRole bool `gorm:"index"`
	// This is whether the user and, all their accounts are banned
	Banned                bool                   `gorm:"index"`
	DiscordUserID         string                 `gorm:"primaryKey"`
	DiscordMinecraftUsers []DiscordMinecraftUser `gorm:"foreignKey:DiscordUserID"`
}

type DiscordMinecraftUser struct {
	gorm.Model
	DiscordUserID string
	MinecraftUser MinecraftUser `gorm:"foreignKey:Username"`
}

type MinecraftUser struct {
	gorm.Model
	Username       string `gorm:"primaryKey"`
	LastLoginTime  time.Time
	LastX          float32
	LastY          float32
	LastZ          float32
	LastIpAddress  pgtype.Inet `gorm:"type:inet"`
	LastChunkImage []byte
	LastSkinImage  []byte
}

// This is a user that has been banned (not a discord user but a minecraft user) - this allows for them to
// not be registered by other players - i.e: the banned users friends
type BannedUser struct {
	gorm.Model
	Username string `gorm:primaryKey`
}

func reportMigrateError(err error) {
	if err != nil {
		log.Print(err)
	}
}

func AutoMigrateModel() {
	reportMigrateError(db.AutoMigrate(&BannedUser{}))
	reportMigrateError(db.AutoMigrate(&MinecraftUser{}))
	reportMigrateError(db.AutoMigrate(&DiscordMinecraftUser{}))
	reportMigrateError(db.AutoMigrate(&DiscordUser{}))
	reportMigrateError(db.AutoMigrate(&GuildSettings{}))
}

// Helper function to set IP addresses, probably won't be used lmao
func SetInet(ip string) pgtype.Inet {
	var inet pgtype.Inet
	inet.Set(ip)
	return inet
}

type Context struct {
	client      *gateway.Session
	interaction *discord.Interaction
}

type Command interface {
	Name() string
	Description() string
	Category() string
	Options() []*discord.ApplicationCommandOption
	Execute(ctx *Context) bool
}

func Register(cmd Command, client *gateway.Session, commands map[string]Command) {
	appCmd := &discord.ApplicationCommand{
		Name:        cmd.Name(),
		Type:        discord.ApplicationCommandChat,
		Description: cmd.Description(),
		Options:     cmd.Options(),
	}

	_, err := client.Application.RegisterCommand(client.Me().Id, "", appCmd)
	if err != nil {
		log.Printf("Error registering command '%s' - %s", cmd.Name(), err)
	}
	commands[cmd.Name()] = cmd
}

func ThemeEmbed(e *embed.Builder, ctx *Context) {
	e.SetFooter(ctx.client.Me().Username, ctx.client.Me().AvatarURL())
	e.SetColor(embed.Green)
}

func SendError(message string, ctx *Context) {
	e := embed.NewEmbedBuilder()

	e.SetTitle("An Error Occurred During Your Command")
	e.SetDescription(message)
	e.SetThumbnail(ctx.interaction.Member.User.AvatarURL())
	ThemeEmbed(e, ctx)

	// Send response
	ctx.client.Interaction.CreateResponse(ctx.interaction.Id,
		ctx.interaction.Token,
		&discord.InteractionCallbackMessage{Embeds: []*embed.Embed{e.Embed()},
			Flags: discord.MessageFlagEphemeral})
}

func SendAdminPermissionsError(gs GuildSettings, ctx *Context) {
	SendError(fmt.Sprintf("You require the <@%s> role to perform this command.", gs.AdminRole), ctx)
}

func SendPermissionsError(gs GuildSettings, ctx *Context) {
	SendError(fmt.Sprintf("You require the <@%s> role to perform this command.", gs.AccessRole), ctx)
}

func SendBannedError(ctx *Context) {
	SendError("You cannot use this command as you have been banned from using the server", ctx)
}

func SendWrongGuildError(ctx *Context) {
	SendError("You cannot use this bot from outside of the CompSoc server.", ctx)
}

func SendInternalError(err error, ctx *Context) {
	log.Print(err)
	SendError(fmt.Sprintf("An internal error has occurred:\n```\n%s\n```", err), ctx)
}

func CheckGuild(ctx *Context) error {
	guildid := ctx.interaction.GuildId
	if guildid == COMPSOC_GUILD_ID {
		SendWrongGuildError(ctx)
		log.Printf("Guild %s is not the CompSoc guild (%s).", guildid, COMPSOC_GUILD_ID)
		return errors.New("Wrong guild.")
	}
	return nil
}
