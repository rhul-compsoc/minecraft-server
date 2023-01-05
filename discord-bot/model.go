package main

import (
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
	AccessRoles string
	// A master toggle to turn off user registrations
	AllowUserRegistrations bool
	MaxAccountsPerUser     uint
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

// Helper function to set IP addresses, probably won't be used lmao
func SetInet(ip string) pgtype.Inet {
	var inet pgtype.Inet
	inet.Set(ip)
	return inet
}

// This is a user that has been banned (not a discord user but a minecraft user) - this allows for them to
// not be registered by other players - i.e: the banned users friends
type BannedUser struct {
	gorm.Model
	Username string `gorm:primaryKey`
}

func AutoMigrateModel() {
	db.AutoMigrate(&BannedUser{})
	db.AutoMigrate(&MinecraftUser{})
	db.AutoMigrate(&DiscordUser{})
	db.AutoMigrate(&GuildSettings{})
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
		log.Print(err)
	}
	commands[cmd.Name()] = cmd
}

func ThemeEmbed(e *embed.Builder, ctx *Context) {
	e.SetFooter(ctx.client.Me().Username, ctx.client.Me().AvatarURL())
	e.SetColor(embed.Green)
}

func SendDatabaseError(ctx *Context) {
	e := embed.NewEmbedBuilder()

	e.SetTitle("An Error Occurred During Your Command")
	e.SetDescription("A database error occured.")
	ThemeEmbed(e, ctx)

	// Send response
	ctx.client.Interaction.CreateResponse(ctx.interaction.Id,
		ctx.interaction.Token,
		&discord.InteractionCallbackMessage{Embeds: []*embed.Embed{e.Embed()},
			Flags: discord.MessageFlagEphemeral})
}

func SendPermissionsError(ctx *Context) {
	e := embed.NewEmbedBuilder()

	e.SetTitle("This Command Requires Administrator Permissions To Run")
	ThemeEmbed(e, ctx)

	// Send response
	ctx.client.Interaction.CreateResponse(ctx.interaction.Id,
		ctx.interaction.Token,
		&discord.InteractionCallbackMessage{Embeds: []*embed.Embed{e.Embed()},
			Flags: discord.MessageFlagEphemeral})
}

func SendError(message string, ctx *Context) {
	e := embed.NewEmbedBuilder()

	e.SetTitle("An Error Occurred During Your Command")
	e.SetDescription(message)
	ThemeEmbed(e, ctx)

	// Send response
	ctx.client.Interaction.CreateResponse(ctx.interaction.Id,
		ctx.interaction.Token,
		&discord.InteractionCallbackMessage{Embeds: []*embed.Embed{e.Embed()},
			Flags: discord.MessageFlagEphemeral})
}
