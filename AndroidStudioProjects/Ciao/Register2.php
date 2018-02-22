<?php
    
    $connect = mysqli_connect("databases.000webhost.com", "id4134628_nathanealv", "Roby1976", "id4134628_ciao_chat_app_database");
    
    $name = $_POST["name"];
    $age = $_POST["age"];
    $email = $_POST["email"];
    $username = $_POST["username"];
    $password = $_POST["password"];

     function registerUser() {
        global $connect, $name, $age, $email, $username, $password;
        $passwordHash = password_hash($password, PASSWORD_DEFAULT);
        $statement = mysqli_prepare($connect, "INSERT INTO user (name, age, email, username, password) VALUES (?, ?, ?, ?, ?)");
        mysqli_stmt_bind_param($statement, "sisss", $name, $age, $email, $username, $passwordHash);
        mysqli_stmt_execute($statement);
        mysqli_stmt_close($statement);     
    }

    function usernameAvailable() {
        global $connect, $username;
        $statement = mysqli_prepare($connect, "SELECT * FROM user WHERE username = ?"); 
        mysqli_stmt_bind_param($statement, "s", $username);
        mysqli_stmt_execute($statement);
        mysqli_stmt_store_result($statement);
        $count = mysqli_stmt_num_rows($statement);
        mysqli_stmt_close($statement); 
        if ($count < 1){
            return true; 
        }else {
            return false; 
        }
    }

    $response = array();
    $response["success"] = false;  

    if (usernameAvailable()){
        registerUser();
        $response["success"] = true;  
    }
    
    print_r(json_encode($response));
?>
