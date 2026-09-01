/* Refresh Plex */
def host = '127.0.0.1'
def auth = 'YOUR_PLEX_TOKEN'

curl "http://${host}:32400/library/sections/all/refresh?X-Plex-Token=${auth}"



/* Refresh Jellyfin */
def host = '127.0.0.1'
def auth = 'YOUR_API_KEY'

curl "http://${host}:8096/Library/Refresh?api_key=${auth}", [:]



/* Refresh Kodi */
def host = '127.0.0.1'
def port = 8080

curl "http://${host}:${port}/jsonrpc", [jsonrpc: '2.0', method: 'VideoLibrary.Scan', id: 1]



/* Run Command */
{ source, target ->
	system '/path/to/script.sh', source, target
}
