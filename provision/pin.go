package provision

// Pinned is the node-install.sh this app build runs: the file at a fixed
// commit of the app's own repository and its SHA-256. TestPinnedScriptHash
// keeps the hash in step with deploy/node-install.sh; when the script
// changes, commit it, then point PinnedCommit at that commit.
const (
	PinnedRepo   = "meepo161/openfluxandroidfork"
	PinnedCommit = "31b05beeba4e992b53d056302598dacd60c3b997"
	PinnedSHA256 = "f7027e38eb17cbc51f9412b4df063e6678b8cfded54f49e418ca2b7fc3447290"
)

// Pinned returns the script location for this build.
func Pinned() Script {
	return Script{
		URL:    "https://raw.githubusercontent.com/" + PinnedRepo + "/" + PinnedCommit + "/deploy/node-install.sh",
		SHA256: PinnedSHA256,
	}
}
