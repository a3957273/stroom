// import useUrlFactory from "../useUrlFactory";
import { Api, RequestParams } from "api/stroom";
import * as React from "react";
import { usePrompt } from "components/Prompt/PromptDisplayBoundary";
// import { useCallback } from "react";
// import {HttpResponse} from "@pollyjs/adapter-fetch";
// import * as React from "react";

const apiPath = process.env.REACT_APP_API_PATH;

export const baseApiParams: RequestParams = {
  credentials: "same-origin",
  headers: {
    "Content-Type": "application/json",
  },
  redirect: "follow",
  referrerPolicy: "no-referrer",
  // mode: "cors",
  // credentials: "include",
};

export const apiConfig = {
  baseUrl: apiPath,
  baseApiParams: baseApiParams,
};

// const api: Api = new Api({
//   baseUrl: apiPath,
//   baseApiParams: baseApiParams,
// });

// type HttpGet = (url: string) => Promise<any>;
// type HttpPost = (url: string, object: any) => Promise<any>;
// type HttpPut = (url: string, object: any) => Promise<any>;
// type HttpDelete = (url: string) => Promise<any>;

interface HttpResponse<D extends unknown, E extends unknown = unknown>
  extends Response {
  data: D;
  error: E;
}

// interface Consumer<T> {
//   <T>(arg: T): void;
// }

type ApiConsumer<T> = (api: Api) => Promise<HttpResponse<T>>;
type Consumer<T> = (arg: T) => void;
type Exec<T> = (
  // promise: Promise<HttpResponse<T>>,
  apiConsumer: ApiConsumer<T>,
  consumer: Consumer<T>,
) => void;
type StroomApi<T> = {
  exec: Exec<T>;
};

// interface StroomApi<T> {
//   handle: Handle<T>;
// }

export const useStroomApi = <T>(): StroomApi<T> => {
  const { showError } = usePrompt();

  const catchImpl = React.useCallback(
    (error: any) => {
      const msg = `Error, Status ${error.status}, Msg: ${error.message}`;
      console.log(msg);
      if (error.message !== undefined) {
        try {
          const json = JSON.parse(error.message);
          showError({ message: json.message });
        } catch (e) {
          showError({ message: error.message });
        }
      } else {
        showError({ message: msg });
      }
    },
    [showError],
  );

  // function handle<T>(apiConsumer: ApiConsumer<T>, consumer: Consumer<T>): void {
  //   const api = new Api(apiConfig);
  //   apiConsumer(api)
  //     .then((r) => {
  //       if (!r.ok) {
  //         console.log(
  //           "Error " + r.status + " - " + r.statusText,
  //           " (" + r.url + ")",
  //         );
  //       } else {
  //         consumer(r.data);
  //       }
  //     })
  //     .catch(catchImpl);
  // }

  const exec = React.useCallback(
    <T>(apiConsumer: ApiConsumer<T>, consumer: Consumer<T>): void => {
      const api = new Api(apiConfig);
      apiConsumer(api)
        .then((r) => {
          if (!r.ok) {
            console.log(
              "Error " + r.status + " - " + r.statusText,
              " (" + r.url + ")",
            );
          } else {
            consumer(r.data);
          }
        })
        .catch(catchImpl);
    },
    [showError, catchImpl],
  );

  return {
    exec,
  };
};

// export class Stroom<T> {
//   private catchImpl = React.useCallback((error: any) => {
//     const msg = `Error, Status ${error.status}, Msg: ${error.message}`;
//     const { showError } = usePrompt();
//     console.log(msg);
//     if (error.message !== undefined) {
//       try {
//         const json = JSON.parse(error.message);
//         showError({ message: json.message });
//       } catch (e) {
//         showError({ message: error.message });
//       }
//     } else {
//       showError({ message: msg });
//     }
//   }, []);
//
//   public call = (apiConsumer: ApiConsumer<T>, consumer: Consumer<T>): void => {
//     const api = new Api(apiConfig);
//     apiConsumer(api)
//       .then((r) => {
//         if (!r.ok) {
//           console.log(
//             "Error " + r.status + " - " + r.statusText,
//             " (" + r.url + ")",
//           );
//         } else {
//           consumer(r.data);
//         }
//       })
//       .catch(this.catchImpl);
//   };
// }

// const { showError } = usePrompt();
//
// const catchImpl = React.useCallback(
//     (error: any) => {
//       const msg = `Error, Status ${error.status}, Msg: ${error.message}`;
//       console.log(msg);
//       if (error.message !== undefined) {
//         try {
//           const json = JSON.parse(error.message);
//           showError({ message: json.message });
//         } catch (e) {
//           showError({ message: error.message });
//         }
//       } else {
//         showError({ message: msg });
//       }
//     },
//     [showError],
// );
//
// function process<T>(promise: Promise<HttpResponse<T>>, consumer: Consumer<T>): void {
//   promise
//       .then((r) => {
//         if (!r.ok) {
//           console.log(
//               "Error " +
//               r.status +
//               " - " +
//               r.statusText,
//               " (" + r.url + ")",
//           );
//
//         } else {
//           consumer(r.data);
//         }
//       })
//       .catch(catchImpl);
// }
//
// const handle: <T>(promise: Promise<T>) => {
//   return "";
//   // promise.then((v) => v.data);
// }
//
// export default useStroomApi;
