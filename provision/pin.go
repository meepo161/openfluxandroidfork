package provision

// Pinned is the node-install.sh this app build runs: the file at a fixed
// commit of the app's own repository and its SHA-256. TestPinnedScriptHash
// keeps the hash in step with deploy/node-install.sh; when the script
// changes, commit it, then point PinnedCommit at that commit.
const (
	PinnedRepo   = "meepo161/openfluxandroidfork"
	PinnedCommit = "69a8a0e07edb63a615dec4a698e0a08172fd8e84"
	PinnedSHA256 = "c15dede39c36638e4bfd1a100ffa89ccdb8f4305d1891b34efb9d88b8c969684"
)

// Pinned returns the script location for this build.
func Pinned() Script {
	return Script{
		URL:    "https://raw.githubusercontent.com/" + PinnedRepo + "/" + PinnedCommit + "/deploy/node-install.sh",
		SHA256: PinnedSHA256,
	}
}
