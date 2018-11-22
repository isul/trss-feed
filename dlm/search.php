<?php
class SynoDLM_TRSS {
	private $qurl = "http://localhost:88/rss/feed?search=";

	public function __construct() {
        date_default_timezone_set('Asia/Seoul');
	}

	public function prepare($curl, $query) {
		$url = $this->qurl.urlencode($query);
		curl_setopt($curl, CURLOPT_URL, $url);
	}

	public function parse($plugin, $response) {
		return $plugin->addRSSResults($response);
	}
}
?>
