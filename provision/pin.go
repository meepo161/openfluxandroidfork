package provision

// Pinned is the node-install.sh this app build runs: the file at a fixed
// commit of the app's own repository and its SHA-256. TestPinnedScriptHash
// keeps the hash in step with deploy/node-install.sh; when the script
// changes, commit it, then point PinnedCommit at that commit.
const (
	PinnedRepo   = "meepo161/openfluxandroidfork"
	PinnedCommit = "9d5add66eb6026efa7a06853fb1cefdcdac2af47"
	PinnedSHA256 = "3d5d0dca9a9c41bbd289ceae20f9de6fceebc1c617a2753a77414a5e5ca8a7af"
)

// Pinned returns the script location for this build.
func Pinned() Script {
	return Script{
		URL:    "https://raw.githubusercontent.com/" + PinnedRepo + "/" + PinnedCommit + "/deploy/node-install.sh",
		SHA256: PinnedSHA256,
	}
}
