import { ChakraProvider, ColorModeScript } from '@chakra-ui/react'
import { RouterProvider } from 'react-router-dom';
import { useEffect, useState, createContext } from 'react';

import { ConversionProvider } from './contexts/Conversion';
import AppRouter from './utils/AppRouter';
import ChakraUITheme from './utils/ChakraUITheme';
import SplashPage from './pages/SplashPage';

const FOUR_SECONDS = 4000;
export const LoggedInContext = createContext()


const App = () => {
    const [ isLoading, setIsLoading ] = useState(false);
    useEffect(() => {
        setTimeout(() => {
            setIsLoading(false);
        }, FOUR_SECONDS);
    }, []);

    const [loggedIn, setLoggedIn] = useState(
        sessionStorage.access ? true : false
    )

    function setLoggedInAndStorage(status) {
        setLoggedIn(status)
        if (status === false) {
            sessionStorage.clear()
        }
    }

    const logOut = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/v1/auth/logout', {
                method: 'POST',
                headers: { 
                    "Authorization": sessionStorage.getItem('access'),
                    "Content-Type": "application/json"
                    },
                    body: {}
            });
            if (!response.ok) {
                console.error("error logging out")
                return;
            }
            setLoggedInAndStorage(false)
        } catch(error) {
            console.error("error logging out", error)
        }
    }

    return (
        isLoading 
        ? <SplashPage /> 
        :
        <LoggedInContext.Provider value={[loggedIn, setLoggedInAndStorage, logOut]}>
            <ConversionProvider>
                <ChakraProvider theme={ChakraUITheme}>
                    <ColorModeScript initialColorMode={ChakraUITheme.config.initialColorMode}/>
                        <RouterProvider router={AppRouter}/>
                </ChakraProvider>
            </ConversionProvider>
        </LoggedInContext.Provider>        
    );
};

export default App;
