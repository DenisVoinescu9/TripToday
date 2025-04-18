async function getApi2Token() {
    const clientId = "yDqKD2YmmwBCbMgx3hEq9P8YZ2jX7B99";
    const clientSecret = "30QJIcLVHKOyD7G_4Arnfb5WK5B1ZN6bS2XP64X6dg3TJ7T36rMmcolOtNFrougW";
    const audience = "https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/";
    const domain = "https://dev-an6hxzzvf6uoryjw.us.auth0.com";

    const url = `${domain}/oauth/token`;

    const body = new URLSearchParams({
        grant_type: "client_credentials",
        client_id: clientId,
        client_secret: clientSecret,
        audience: audience,
    });

    try {
        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
            },
            body: body.toString(),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }

        const data = await response.json();
        return data.access_token;
    } catch (error) {
        console.error("Error getting token:", error);
    }
}

async function updatePicture(userId, imageUrl) {
    const token = await getApi2Token();

    if (!token) {
        console.error("Failed to retrieve token!");
        return;
    }

    const url = `https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/users/${userId}`;
    const body = { picture: imageUrl };

    try {
        const response = await fetch(url, {
            method: "PATCH",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(body)
        });


        alert("Profile picture updated successfully!")
        console.log("Profile picture updated successfully!");

    } catch (error) {
        console.error("Error updating picture:", error);
    }
}

async function updateGuideDescription(userId, description) {
    const token = await getApi2Token();
    if (!token) return console.error("Failed to retrieve token!");

    const url = `https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/users/${userId}`;
    const body = { "user_metadata": { "description": description } };

    try {
        await fetch(url, {
            method: "PATCH",
            headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });

        alert("Guide description updated successfully!");
    } catch (error) {
        console.error("Error updating description:", error);
    }
}



